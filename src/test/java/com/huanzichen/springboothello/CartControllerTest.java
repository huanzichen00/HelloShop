package com.huanzichen.springboothello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> cartItemIds = new ArrayList<>();
    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
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
        }
        cartItemIds.clear();
        productIds.clear();
        categoryIds.clear();
        userIds.clear();
    }

    @Test
    void shouldRequireLoginWhenListingCart() throws Exception {
        mockMvc.perform(get("/cart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldAddCartItemAndListCurrentUserCart() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "cart_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("cart-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "cart-product-" + suffix, new BigDecimal("29.90"), 20, "ON_SALE");

        mockMvc.perform(withToken(post("/cart/items"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 2
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productId").value(productId))
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.selected").value(true))
                .andExpect(jsonPath("$.data.productName").value("cart-product-" + suffix))
                .andExpect(jsonPath("$.data.productPrice").value(29.9));

        Long cartItemId = findCartItemId(userId, productId);
        cartItemIds.add(cartItemId);

        mockMvc.perform(withToken(get("/cart"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(cartItemId))
                .andExpect(jsonPath("$.data[0].productId").value(productId))
                .andExpect(jsonPath("$.data[0].quantity").value(2))
                .andExpect(jsonPath("$.data[0].productStatus").value("ON_SALE"));
    }

    @Test
    void shouldMergeQuantityWhenAddingSameProductAgain() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "cart_merge_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("merge-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "merge-product-" + suffix, new BigDecimal("49.90"), 20, "ON_SALE");

        mockMvc.perform(withToken(post("/cart/items"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 2
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(2));

        mockMvc.perform(withToken(post("/cart/items"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 3
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(5))
                .andExpect(jsonPath("$.data.selected").value(true));

        Long cartItemId = findCartItemId(userId, productId);
        cartItemIds.add(cartItemId);
    }

    @Test
    void shouldUpdateCartItemQuantityAndSelected() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "cart_update_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("update-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "update-product-" + suffix, new BigDecimal("59.90"), 20, "ON_SALE");
        Long cartItemId = insertCartItem(userId, productId, 2, true);

        mockMvc.perform(withToken(put("/cart/items/" + cartItemId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 4,
                                  "selected": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(cartItemId))
                .andExpect(jsonPath("$.data.quantity").value(4))
                .andExpect(jsonPath("$.data.selected").value(false));
    }

    @Test
    void shouldDeleteCartItem() throws Exception {
        long suffix = System.currentTimeMillis();
        String username = "cart_delete_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long categoryId = insertCategory("delete-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "delete-product-" + suffix, new BigDecimal("69.90"), 20, "ON_SALE");
        Long cartItemId = insertCartItem(userId, productId, 1, true);

        mockMvc.perform(withToken(delete("/cart/items/" + cartItemId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from cart_items where id = ?",
                Integer.class,
                cartItemId
        );
        if (count != null && count == 0) {
            cartItemIds.remove(cartItemId);
        }
    }

    private String registerAndLogin(String username) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "password": "123456",
                  "name": "cart user",
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

    private Long findCartItemId(Long userId, Long productId) {
        return jdbcTemplate.queryForObject(
                "select id from cart_items where user_id = ? and product_id = ?",
                Long.class,
                userId,
                productId
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
}
