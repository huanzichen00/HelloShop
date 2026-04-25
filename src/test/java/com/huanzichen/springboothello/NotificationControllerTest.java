package com.huanzichen.springboothello;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huanzichen.springboothello.consumer.OrderCreatedConsumer;
import com.huanzichen.springboothello.dto.order.OrderCreatedMessage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderCreatedConsumer orderCreatedConsumer;

    private final List<Long> notificationIds = new ArrayList<>();
    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long notificationId : notificationIds) {
            jdbcTemplate.update("delete from notifications where id = ?", notificationId);
        }
        for (Long orderId : orderIds) {
            jdbcTemplate.update("delete from orders where id = ?", orderId);
        }
        for (Long userId : userIds) {
            jdbcTemplate.update("delete from users where id = ?", userId);
        }

        notificationIds.clear();
        orderIds.clear();
        userIds.clear();
    }

    @Test
    void shouldRequireLoginWhenListingNotifications() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldRequireLoginWhenListingNotificationsByPage() throws Exception {
        mockMvc.perform(get("/notifications/page").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldRequireLoginWhenCountingUnreadNotifications() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldRequireLoginWhenMarkingAllNotificationsAsRead() throws Exception {
        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("please login first"));
    }

    @Test
    void shouldListOnlyCurrentUsersNotificationsOrderedByCreatedAtDescThenIdDesc() throws Exception {
        long suffix = System.nanoTime();
        String firstUsername = "notification_user_a_" + suffix;
        String secondUsername = "notification_user_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        Long firstNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "same-time-first", "first content", false);
        Long secondNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "same-time-second", "second content", false);
        Long newestNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "newest-notification", "newest content", true);
        Long otherNotificationId = insertNotification(secondUserId, null, "ORDER_CREATED", "other-users-notification", "other content", false);

        setNotificationCreatedAt(firstNotificationId, "2035-01-01 10:00:00");
        setNotificationCreatedAt(secondNotificationId, "2035-01-01 10:00:00");
        setNotificationCreatedAt(newestNotificationId, "2035-01-01 11:00:00");
        setNotificationCreatedAt(otherNotificationId, "2035-01-01 12:00:00");

        String response = mockMvc.perform(withToken(get("/notifications"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].title").value("newest-notification"))
                .andExpect(jsonPath("$.data[1].title").value("same-time-second"))
                .andExpect(jsonPath("$.data[2].title").value("same-time-first"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!response.contains("other-users-notification"), "should not return other user's notifications");
    }

    @Test
    void shouldPageOnlyCurrentUsersNotificationsOrderedByCreatedAtDescThenIdDesc() throws Exception {
        long suffix = System.nanoTime();
        String firstUsername = "notification_page_user_a_" + suffix;
        String secondUsername = "notification_page_user_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        Long firstNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "page-first", "first content", false);
        Long secondNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "page-second", "second content", false);
        Long thirdNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "page-third", "third content", false);
        Long otherNotificationId = insertNotification(secondUserId, null, "ORDER_CREATED", "page-other", "other content", false);

        setNotificationCreatedAt(firstNotificationId, "2038-01-01 10:01:00");
        setNotificationCreatedAt(secondNotificationId, "2038-01-01 10:02:00");
        setNotificationCreatedAt(thirdNotificationId, "2038-01-01 10:03:00");
        setNotificationCreatedAt(otherNotificationId, "2038-01-01 10:04:00");

        String response = mockMvc.perform(withToken(get("/notifications/page")
                        .param("page", "1")
                        .param("size", "2"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.list.length()").value(2))
                .andExpect(jsonPath("$.data.list[0].title").value("page-third"))
                .andExpect(jsonPath("$.data.list[1].title").value("page-second"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!response.contains("page-other"), "should not return other user's notifications");
    }

    @Test
    void shouldFilterPagedNotificationsByIsRead() throws Exception {
        long suffix = System.nanoTime();
        String username = "notify_read_f_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long unreadNotificationId = insertNotification(userId, null, "ORDER_CREATED", "unread-filter", "unread content", false);
        Long readNotificationId = insertNotification(userId, null, "ORDER_CREATED", "read-filter", "read content", true);

        setNotificationCreatedAt(unreadNotificationId, "2039-01-01 10:01:00");
        setNotificationCreatedAt(readNotificationId, "2039-01-01 10:02:00");

        mockMvc.perform(withToken(get("/notifications/page")
                        .param("page", "1")
                        .param("size", "10")
                        .param("isRead", "false"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(unreadNotificationId))
                .andExpect(jsonPath("$.data.list[0].isRead").value(false));
    }

    @Test
    void shouldFilterPagedNotificationsByType() throws Exception {
        long suffix = System.nanoTime();
        String firstUsername = "notify_type_a_" + suffix;
        String secondUsername = "notify_type_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        Long createdNotificationId = insertNotification(firstUserId, null, "ORDER_CREATED", "created-filter", "created content", false);
        Long timeoutNotificationId = insertNotification(firstUserId, null, "ORDER_TIMEOUT_CANCELED", "timeout-filter", "timeout content", false);
        Long otherUserNotificationId = insertNotification(secondUserId, null, "ORDER_CREATED", "other-created-filter", "other content", false);

        setNotificationCreatedAt(createdNotificationId, "2040-01-01 10:01:00");
        setNotificationCreatedAt(timeoutNotificationId, "2040-01-01 10:02:00");
        setNotificationCreatedAt(otherUserNotificationId, "2040-01-01 10:03:00");

        String response = mockMvc.perform(withToken(get("/notifications/page")
                        .param("page", "1")
                        .param("size", "10")
                        .param("type", "ORDER_CREATED"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list.length()").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(createdNotificationId))
                .andExpect(jsonPath("$.data.list[0].type").value("ORDER_CREATED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!response.contains("timeout-filter"), "should not return other notification types");
        assertTrue(!response.contains("other-created-filter"), "should not return other user's notifications");
    }

    @Test
    void shouldCountOnlyCurrentUsersUnreadNotifications() throws Exception {
        long suffix = System.nanoTime();
        String firstUsername = "notification_count_user_a_" + suffix;
        String secondUsername = "notification_count_user_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        insertNotification(firstUserId, null, "ORDER_CREATED", "unread-a-1", "unread content", false);
        insertNotification(firstUserId, null, "ORDER_CREATED", "unread-a-2", "unread content", false);
        insertNotification(firstUserId, null, "ORDER_CREATED", "read-a", "read content", true);
        insertNotification(secondUserId, null, "ORDER_CREATED", "unread-b", "other unread content", false);

        mockMvc.perform(withToken(get("/notifications/unread-count"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    void shouldMarkOwnNotificationAsRead() throws Exception {
        long suffix = System.nanoTime();
        String username = "notification_read_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long notificationId = insertNotification(userId, null, "ORDER_CREATED", "read-title", "read content", false);

        mockMvc.perform(withToken(put("/notifications/" + notificationId + "/read"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(notificationId))
                .andExpect(jsonPath("$.data.isRead").value(true));

        Integer isRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                notificationId
        );
        assertEquals(1, isRead);
    }

    @Test
    void shouldMarkAllOwnUnreadNotificationsAsRead() throws Exception {
        long suffix = System.nanoTime();
        String firstUsername = "notification_read_all_user_a_" + suffix;
        String secondUsername = "notification_read_all_user_b_" + suffix;
        String firstToken = registerAndLogin(firstUsername);
        registerAndLogin(secondUsername);

        Long firstUserId = findUserIdByUsername(firstUsername);
        Long secondUserId = findUserIdByUsername(secondUsername);

        Long firstUnreadId = insertNotification(firstUserId, null, "ORDER_CREATED", "first unread", "first unread content", false);
        Long secondUnreadId = insertNotification(firstUserId, null, "ORDER_CREATED", "second unread", "second unread content", false);
        Long firstReadId = insertNotification(firstUserId, null, "ORDER_CREATED", "already read", "already read content", true);
        Long otherUnreadId = insertNotification(secondUserId, null, "ORDER_CREATED", "other unread", "other unread content", false);

        mockMvc.perform(withToken(put("/notifications/read-all"), firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(2));

        Integer firstUnreadIsRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                firstUnreadId
        );
        Integer secondUnreadIsRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                secondUnreadId
        );
        Integer firstReadIsRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                firstReadId
        );
        Integer otherUnreadIsRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                otherUnreadId
        );

        assertEquals(1, firstUnreadIsRead);
        assertEquals(1, secondUnreadIsRead);
        assertEquals(1, firstReadIsRead);
        assertEquals(0, otherUnreadIsRead);
    }

    @Test
    void shouldReturn404WhenReadingOtherUsersNotification() throws Exception {
        long suffix = System.nanoTime();
        String ownerUsername = "notification_owner_" + suffix;
        String otherUsername = "notification_other_" + suffix;
        registerAndLogin(ownerUsername);
        String otherToken = registerAndLogin(otherUsername);

        Long ownerUserId = findUserIdByUsername(ownerUsername);
        Long notificationId = insertNotification(ownerUserId, null, "ORDER_CREATED", "private-title", "private content", false);

        mockMvc.perform(withToken(put("/notifications/" + notificationId + "/read"), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("notification not found"));
    }

    @Test
    void shouldDeleteOwnNotification() throws Exception {
        long suffix = System.nanoTime();
        String username = "notification_delete_user_" + suffix;
        String token = registerAndLogin(username);
        Long userId = findUserIdByUsername(username);

        Long notificationId = insertNotification(userId, null, "ORDER_CREATED", "delete-title", "delete content", false);

        mockMvc.perform(withToken(delete("/notifications/" + notificationId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").doesNotExist());

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from notifications where id = ?",
                Integer.class,
                notificationId
        );
        assertEquals(0, count);
        notificationIds.remove(notificationId);
    }

    @Test
    void shouldReturn404WhenDeletingOtherUsersNotification() throws Exception {
        long suffix = System.nanoTime();
        String ownerUsername = "notification_delete_owner_" + suffix;
        String otherUsername = "notification_delete_other_" + suffix;
        registerAndLogin(ownerUsername);
        String otherToken = registerAndLogin(otherUsername);

        Long ownerUserId = findUserIdByUsername(ownerUsername);
        Long notificationId = insertNotification(ownerUserId, null, "ORDER_CREATED", "delete-private", "private content", false);

        mockMvc.perform(withToken(delete("/notifications/" + notificationId), otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("notification not found"));
    }

    @Test
    void shouldCreateNotificationWhenHandlingOrderCreatedMessage() {
        long suffix = System.nanoTime();
        String username = "notification_consumer_user_" + suffix;
        Long userId = insertUser(username);
        Long orderId = insertOrder(userId, "MQ-" + suffix, "PENDING_PAYMENT", new BigDecimal("99.90"), 1);

        OrderCreatedMessage message = new OrderCreatedMessage();
        message.setUserId(userId);
        message.setOrderId(orderId);
        message.setOrderNo("MQ-" + suffix);
        message.setTotalAmount(new BigDecimal("99.90"));
        message.setTotalQuantity(1);

        orderCreatedConsumer.handleOrderCreated(message);

        List<Long> createdNotificationIds = jdbcTemplate.query(
                "select id from notifications where user_id = ? and order_id = ? order by id asc",
                (rs, rowNum) -> rs.getLong("id"),
                userId,
                orderId
        );
        notificationIds.addAll(createdNotificationIds);

        Long notificationId = jdbcTemplate.queryForObject(
                "select id from notifications where user_id = ? and order_id = ?",
                Long.class,
                userId,
                orderId
        );
        String type = jdbcTemplate.queryForObject(
                "select type from notifications where id = ?",
                String.class,
                notificationId
        );
        String title = jdbcTemplate.queryForObject(
                "select title from notifications where id = ?",
                String.class,
                notificationId
        );
        String content = jdbcTemplate.queryForObject(
                "select content from notifications where id = ?",
                String.class,
                notificationId
        );
        Integer isRead = jdbcTemplate.queryForObject(
                "select is_read from notifications where id = ?",
                Integer.class,
                notificationId
        );

        assertEquals("ORDER_CREATED", type);
        assertEquals("订单创建成功", title);
        assertTrue(content.contains("MQ-" + suffix), "notification content should contain order number");
        assertEquals(0, isRead);
    }

    private String registerAndLogin(String username) throws Exception {
        String registerBody = """
                {
                  "username": "%s",
                  "password": "123456",
                  "name": "notification user",
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

    private Long insertUser(String username) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into users(username, password, name, age) values (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setString(1, username);
                    ps.setString(2, "$2a$10$abcdefghijklmnopqrstuv123456789012345678901234567890");
                    ps.setString(3, "notification user");
                    ps.setInt(4, 20);
                    return ps;
                },
                keyHolder
        );
        Long userId = keyHolder.getKey().longValue();
        userIds.add(userId);
        return userId;
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
        Long orderId = keyHolder.getKey().longValue();
        orderIds.add(orderId);
        return orderId;
    }

    private Long insertNotification(Long userId, Long orderId, String type, String title, String content, boolean isRead) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                connection -> {
                    PreparedStatement ps = connection.prepareStatement(
                            "insert into notifications(user_id, order_id, type, title, content, is_read) values (?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS
                    );
                    ps.setLong(1, userId);
                    if (orderId == null) {
                        ps.setObject(2, null);
                    } else {
                        ps.setLong(2, orderId);
                    }
                    ps.setString(3, type);
                    ps.setString(4, title);
                    ps.setString(5, content);
                    ps.setBoolean(6, isRead);
                    return ps;
                },
                keyHolder
        );
        Long notificationId = keyHolder.getKey().longValue();
        notificationIds.add(notificationId);
        return notificationId;
    }

    private void setNotificationCreatedAt(Long notificationId, String createdAt) {
        jdbcTemplate.update(
                "update notifications set created_at = ? where id = ?",
                createdAt,
                notificationId
        );
    }
}
