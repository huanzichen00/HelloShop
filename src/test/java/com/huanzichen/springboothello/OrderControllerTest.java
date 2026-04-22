package com.huanzichen.springboothello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class OrderControllerTest {

    private static final String ORDER_SUBMIT_KEY_PREFIX = "order:submit:";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final List<Long> orderItemIds = new ArrayList<>();
    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> cartItemIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long orderItemId : orderItemIds) {
            jdbcTemplate.update("delete from order_items where id = ?", orderItemId);
        }
        for (Long orderId : orderIds) {
            jdbcTemplate.update("delete from orders where id = ?", orderId);
        }
        for (Long cartItemId : cartItemIds) {
            jdbcTemplate.update("delete from cart_items where id = ?", cartItemId);
        }
        for (Long productId : productIds) {
            jdbcTemplate.update("delete from products where id = ?", productId);
        }
        for (Long categoryId : categoryIds) {
            jdbcTemplate.update("delete from categories where id = ?", categoryId);
        }
        for (Long userId : userIds) {
            jdbcTemplate.update("delete from users where id = ?", userId);
            stringRedisTemplate.delete(ORDER_SUBMIT_KEY_PREFIX + userId);
        }

        orderItemIds.clear();
        orderIds.clear();
        cartItemIds.clear();
        productIds.clear();
        categoryIds.clear();
        userIds.clear();
    }

    @Test
    void shouldRequireLoginWhenListingOrders() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldCreateOrderFromSelectedCartItems() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_create_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("order-category-" + suffix, 1);
        Long firstProductId = insertProduct(categoryId, "order-product-a-" + suffix, new BigDecimal("10.50"), 20, "ON_SALE");
        Long secondProductId = insertProduct(categoryId, "order-product-b-" + suffix, new BigDecimal("20.00"), 20, "ON_SALE");

        Long firstCartItemId = insertCartItem(userId, firstProductId, 2, true);
        Long secondCartItemId = insertCartItem(userId, secondProductId, 1, true);

        String response = mockMvc.perform(withToken(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%d, %d]
                                }
                                """.formatted(firstCartItemId, secondCartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(41.0))
                .andExpect(jsonPath("$.data.totalQuantity").value(3))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value("order-product-a-" + suffix))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        Long orderId = jsonNode.get("data").get("id").asLong();
        orderIds.add(orderId);

        List<Long> createdOrderItemIds = jdbcTemplate.query(
                "select id from order_items where order_id = ? order by id asc",
                (rs, rowNum) -> rs.getLong("id"),
                orderId
        );
        orderItemIds.addAll(createdOrderItemIds);

        Integer orderItemCount = jdbcTemplate.queryForObject(
                "select count(*) from order_items where order_id = ?",
                Integer.class,
                orderId
        );
        Integer firstStock = jdbcTemplate.queryForObject(
                "select stock from products where id = ?",
                Integer.class,
                firstProductId
        );
        Integer secondStock = jdbcTemplate.queryForObject(
                "select stock from products where id = ?",
                Integer.class,
                secondProductId
        );
        Integer cartCount = jdbcTemplate.queryForObject(
                "select count(*) from cart_items where id in (?, ?)",
                Integer.class,
                firstCartItemId,
                secondCartItemId
        );

        if (cartCount != null && cartCount == 0) {
            cartItemIds.remove(firstCartItemId);
            cartItemIds.remove(secondCartItemId);
        }

        org.junit.jupiter.api.Assertions.assertEquals(2, orderItemCount);
        org.junit.jupiter.api.Assertions.assertEquals(18, firstStock);
        org.junit.jupiter.api.Assertions.assertEquals(19, secondStock);
        org.junit.jupiter.api.Assertions.assertEquals(0, cartCount);
    }

    @Test
    void shouldReturn400WhenSubmittingOrderRepeatedly() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_repeat_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("repeat-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "repeat-product-" + suffix, new BigDecimal("12.00"), 10, "ON_SALE");
        Long cartItemId = insertCartItem(userId, productId, 1, true);

        stringRedisTemplate.opsForValue().set(ORDER_SUBMIT_KEY_PREFIX + userId, "1");

        mockMvc.perform(withToken(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%d]
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("please do not submit repeatedly"));

        Integer orderCount = jdbcTemplate.queryForObject(
                "select count(*) from orders where user_id = ?",
                Integer.class,
                userId
        );
        org.junit.jupiter.api.Assertions.assertEquals(0, orderCount);
    }

    @Test
    void shouldListOnlyCurrentUsersOrders() throws Exception {
        long suffix = System.currentTimeMillis();
        String firstUsername = "order_list_user_a_" + suffix;
        String secondUsername = "order_list_user_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        Long firstOrderId = insertOrder(firstUserId, "LIST-A-" + suffix, new BigDecimal("30.00"), 2);
        Long secondOrderId = insertOrder(secondUserId, "LIST-B-" + suffix, new BigDecimal("50.00"), 4);

        mockMvc.perform(withToken(get("/orders"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(firstOrderId))
                .andExpect(jsonPath("$.data[0].orderNo").value("LIST-A-" + suffix));

        orderIds.add(firstOrderId);
        orderIds.add(secondOrderId);
    }

    @Test
    void shouldReturn404WhenAccessingOtherUsersOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String ownerUsername = "order_owner_" + suffix;
        String otherUsername = "order_other_" + suffix;
        registerAndLogin(ownerUsername);
        String otherToken = registerAndLogin(otherUsername);

        Long ownerUserId = findUserIdByUsername(ownerUsername);
        Long orderId = insertOrder(ownerUserId, "ORDER-PRIVATE-" + suffix, new BigDecimal("88.00"), 5);

        mockMvc.perform(withToken(get("/orders/" + orderId), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));

        orderIds.add(orderId);
    }

    @Test
    void shouldReturnOrderDetailWithItems() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_detail_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("detail-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "detail-product-" + suffix, new BigDecimal("15.00"), 20, "ON_SALE");
        Long cartItemId = insertCartItem(userId, productId, 2, true);

        String createResponse = mockMvc.perform(withToken(post("/orders"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cartItemIds": [%d]
                                }
                                """.formatted(cartItemId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(createResponse);
        Long orderId = jsonNode.get("data").get("id").asLong();
        orderIds.add(orderId);

        List<Long> createdOrderItemIds = jdbcTemplate.query(
                "select id from order_items where order_id = ? order by id asc",
                (rs, rowNum) -> rs.getLong("id"),
                orderId
        );
        orderItemIds.addAll(createdOrderItemIds);

        mockMvc.perform(withToken(get("/orders/" + orderId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.userId").value(userId))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                .andExpect(jsonPath("$.data.items[0].productName").value("detail-product-" + suffix))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].subtotalAmount").value(30.0));
    }

    @Test
    void shouldCancelPendingPaymentOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_cancel_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("cancel-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "cancel-product-" + suffix, new BigDecimal("22.00"), 10, "ON_SALE");
        Long orderId = insertOrder(userId, "ORDER-CANCEL-" + suffix, "PENDING_PAYMENT", new BigDecimal("66.00"), 3);
        Long orderItemId = insertOrderItem(orderId, productId, "cancel-product-" + suffix, new BigDecimal("22.00"), 3, new BigDecimal("66.00"));
        orderIds.add(orderId);
        orderItemIds.add(orderItemId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/cancel"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("CANCELED"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3));

        String statusValue = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        Integer restoredStock = jdbcTemplate.queryForObject(
                "select stock from products where id = ?",
                Integer.class,
                productId
        );
        org.junit.jupiter.api.Assertions.assertEquals("CANCELED", statusValue);
        org.junit.jupiter.api.Assertions.assertEquals(13, restoredStock);
    }

    @Test
    void shouldReturn404WhenCancelingOtherUsersOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String ownerUsername = "order_cancel_owner_" + suffix;
        String otherUsername = "order_cancel_other_" + suffix;
        registerAndLogin(ownerUsername);
        String otherToken = registerAndLogin(otherUsername);

        Long ownerUserId = findUserIdByUsername(ownerUsername);
        Long orderId = insertOrder(ownerUserId, "OCP-" + suffix, "PENDING_PAYMENT", new BigDecimal("77.00"), 4);
        orderIds.add(orderId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/cancel"), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    @Test
    void shouldReturn400WhenCancelingNonPendingOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_cancel_paid_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long orderId = insertOrder(userId, "OCPAID-" + suffix, "PAID", new BigDecimal("99.00"), 5);
        orderIds.add(orderId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/cancel"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("order cannot be canceled"));
    }

    @Test
    void shouldPayPendingPaymentOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_pay_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("pay-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "pay-product-" + suffix, new BigDecimal("18.00"), 9, "ON_SALE");
        Long orderId = insertOrder(userId, "PAY-" + suffix, "PENDING_PAYMENT", new BigDecimal("36.00"), 2);
        Long orderItemId = insertOrderItem(orderId, productId, "pay-product-" + suffix, new BigDecimal("18.00"), 2, new BigDecimal("36.00"));
        orderIds.add(orderId);
        orderItemIds.add(orderItemId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/pay"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2));

        String statusValue = jdbcTemplate.queryForObject(
                "select status from orders where id = ?",
                String.class,
                orderId
        );
        org.junit.jupiter.api.Assertions.assertEquals("PAID", statusValue);
    }

    @Test
    void shouldReturn404WhenPayingOtherUsersOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String ownerUsername = "order_pay_owner_" + suffix;
        String otherUsername = "order_pay_other_" + suffix;
        registerAndLogin(ownerUsername);
        String otherToken = registerAndLogin(otherUsername);

        Long ownerUserId = findUserIdByUsername(ownerUsername);
        Long orderId = insertOrder(ownerUserId, "PAYO-" + suffix, "PENDING_PAYMENT", new BigDecimal("58.00"), 3);
        orderIds.add(orderId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/pay"), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("order not found"));
    }

    @Test
    void shouldReturn400WhenPayingCanceledOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_pay_canceled_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long orderId = insertOrder(userId, "PAYC-" + suffix, "CANCELED", new BigDecimal("79.00"), 4);
        orderIds.add(orderId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/pay"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldReturn400WhenPayingPaidOrder() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "order_pay_paid_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long orderId = insertOrder(userId, "PAYP-" + suffix, "PAID", new BigDecimal("81.00"), 4);
        orderIds.add(orderId);

        mockMvc.perform(withToken(put("/orders/" + orderId + "/pay"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    private String registerAndLogin(String username) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "password": "123456",
                  "name": "order user",
                  "age": 20
                }
                """.formatted(username);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Long userId = findUserIdByUsername(username);
        userIds.add(userId);

        String loginBody = """
                {
                  "username": "%s",
                  "password": "123456"
                }
                """.formatted(username);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(response);
        return jsonNode.get("data").asText();
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder builder, String token) {
        return builder.header("Authorization", "Bearer " + token);
    }

    private Long findUserIdByUsername(String username) {
        return jdbcTemplate.queryForObject(
                "select id from users where username = ?",
                Long.class,
                username
        );
    }

    private Long insertCategory(String name, int sort) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into categories(name, sort) values (?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, name);
                    ps.setInt(2, sort);
                    return ps;
                },
                keyHolder
        );
        Long categoryId = keyHolder.getKey().longValue();
        categoryIds.add(categoryId);
        return categoryId;
    }

    private Long insertProduct(Long categoryId, String name, BigDecimal price, int stock, String status) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            """
                            insert into products(category_id, name, description, price, stock, status, cover_url)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setLong(1, categoryId);
                    ps.setString(2, name);
                    ps.setString(3, name + " description");
                    ps.setBigDecimal(4, price);
                    ps.setInt(5, stock);
                    ps.setString(6, status);
                    ps.setString(7, "https://example.com/" + name + ".jpg");
                    return ps;
                },
                keyHolder
        );
        Long productId = keyHolder.getKey().longValue();
        productIds.add(productId);
        return productId;
    }

    private Long insertCartItem(Long userId, Long productId, int quantity, boolean selected) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into cart_items(user_id, product_id, quantity, selected) values (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setLong(1, userId);
                    ps.setLong(2, productId);
                    ps.setInt(3, quantity);
                    ps.setBoolean(4, selected);
                    return ps;
                },
                keyHolder
        );
        Long cartItemId = keyHolder.getKey().longValue();
        cartItemIds.add(cartItemId);
        return cartItemId;
    }

    private Long insertOrder(Long userId, String orderNo, BigDecimal totalAmount, int totalQuantity) {
        return insertOrder(userId, orderNo, "PENDING_PAYMENT", totalAmount, totalQuantity);
    }

    private Long insertOrder(Long userId, String orderNo, String status, BigDecimal totalAmount, int totalQuantity) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into orders(order_no, user_id, status, total_amount, total_quantity) values (?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, orderNo);
                    ps.setLong(2, userId);
                    ps.setString(3, status);
                    ps.setBigDecimal(4, totalAmount);
                    ps.setInt(5, totalQuantity);
                    return ps;
                },
                keyHolder
        );
        return keyHolder.getKey().longValue();
    }

    private Long insertOrderItem(Long orderId, Long productId, String productName, BigDecimal productPrice, int quantity, BigDecimal subtotalAmount) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            """
                            insert into order_items(order_id, product_id, product_name, product_price, product_cover_url, quantity, subtotal_amount)
                            values (?, ?, ?, ?, ?, ?, ?)
                            """,
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setLong(1, orderId);
                    ps.setLong(2, productId);
                    ps.setString(3, productName);
                    ps.setBigDecimal(4, productPrice);
                    ps.setString(5, "https://example.com/" + productName + ".jpg");
                    ps.setInt(6, quantity);
                    ps.setBigDecimal(7, subtotalAmount);
                    return ps;
                },
                keyHolder
        );
        return keyHolder.getKey().longValue();
    }
}
