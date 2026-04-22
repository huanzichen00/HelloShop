package com.huanzichen.springboothello;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CatalogControllerTest {

    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final String HOT_PRODUCT_KEY = "product:hot:5";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long productId : productIds) {
            jdbcTemplate.update("delete from products where id = ?", productId);
            stringRedisTemplate.delete(PRODUCT_DETAIL_KEY_PREFIX + productId);
        }
        stringRedisTemplate.delete(HOT_PRODUCT_KEY);
        for (Long categoryId : categoryIds) {
            jdbcTemplate.update("delete from categories where id = ?", categoryId);
        }
        productIds.clear();
        categoryIds.clear();
    }

    @Test
    void shouldListCategoriesOrderedBySortThenId() throws Exception {
        long suffix = System.currentTimeMillis();
        String firstCategoryName = "category-first-" + suffix;
        String secondCategoryName = "category-second-" + suffix;
        String topCategoryName = "category-top-" + suffix;

        insertCategory(firstCategoryName, 10);
        insertCategory(secondCategoryName, 10);
        insertCategory(topCategoryName, 1);

        String response = mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn()
                .getResponse()
                .getContentAsString();

        int topIndex = response.indexOf(topCategoryName);
        int firstIndex = response.indexOf(firstCategoryName);
        int secondIndex = response.indexOf(secondCategoryName);

        assertTrue(topIndex >= 0, "top category should exist in response");
        assertTrue(firstIndex >= 0, "first category should exist in response");
        assertTrue(secondIndex >= 0, "second category should exist in response");
        assertTrue(topIndex < firstIndex, "lower sort category should appear first");
        assertTrue(firstIndex < secondIndex, "same sort categories should be ordered by id asc");
    }

    @Test
    void shouldSearchProductsByCategoryAndKeywordWithPagination() throws Exception {
        long suffix = System.currentTimeMillis();
        Long phoneCategoryId = insertCategory("phones-" + suffix, 1);
        Long otherCategoryId = insertCategory("others-" + suffix, 2);

        insertProduct(phoneCategoryId, "phone-basic-" + suffix, new BigDecimal("99.00"), "ON_SALE");
        insertProduct(phoneCategoryId, "phone-pro-" + suffix, new BigDecimal("199.00"), "ON_SALE");
        insertProduct(otherCategoryId, "other-phone-" + suffix, new BigDecimal("299.00"), "ON_SALE");

        mockMvc.perform(get("/products")
                        .param("categoryId", String.valueOf(phoneCategoryId))
                        .param("keyword", "phone")
                        .param("page", "1")
                        .param("size", "1")
                        .param("sort", "price")
                        .param("order", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.list[0].name").value("phone-pro-" + suffix))
                .andExpect(jsonPath("$.data.list[0].categoryId").value(phoneCategoryId))
                .andExpect(jsonPath("$.data.list[0].categoryName").value("phones-" + suffix));
    }

    @Test
    void shouldReturnProductDetailById() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("detail-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "detail-product-" + suffix, new BigDecimal("88.80"), "ON_SALE");

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("detail-product-" + suffix))
                .andExpect(jsonPath("$.data.categoryId").value(categoryId))
                .andExpect(jsonPath("$.data.categoryName").value("detail-category-" + suffix))
                .andExpect(jsonPath("$.data.price").value(88.8))
                .andExpect(jsonPath("$.data.status").value("ON_SALE"));
    }

    @Test
    void shouldReturn404WhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/products/999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("product not found"));
    }

    @Test
    void shouldCacheProductDetailAfterFirstRead() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("cache-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "cache-product-" + suffix, new BigDecimal("66.60"), "ON_SALE");

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("cache-product-" + suffix));

        String cachedJson = stringRedisTemplate.opsForValue().get(PRODUCT_DETAIL_KEY_PREFIX + productId);
        assertTrue(cachedJson != null && cachedJson.contains("cache-product-" + suffix), "product detail should be written to redis");
    }

    @Test
    void shouldReturnProductDetailFromCacheWhenRedisIsPopulated() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("cache-hit-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "db-product-" + suffix, new BigDecimal("77.70"), "ON_SALE");

        stringRedisTemplate.opsForValue().set(
                PRODUCT_DETAIL_KEY_PREFIX + productId,
                """
                {"id":%d,"categoryId":%d,"categoryName":"cached-category-%d","name":"cached-product-%d","description":"cached description","price":88.8,"stock":99,"status":"ON_SALE","coverUrl":"https://example.com/cached.jpg"}
                """.formatted(productId, categoryId, suffix, suffix)
        );

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("cached-product-" + suffix))
                .andExpect(jsonPath("$.data.categoryName").value("cached-category-" + suffix))
                .andExpect(jsonPath("$.data.price").value(88.8));
    }

    @Test
    void shouldFallbackToDatabaseWhenCachedProductJsonIsInvalid() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("cache-fallback-category-" + suffix, 1);
        Long productId = insertProduct(categoryId, "fallback-product-" + suffix, new BigDecimal("55.50"), "ON_SALE");

        stringRedisTemplate.opsForValue().set(PRODUCT_DETAIL_KEY_PREFIX + productId, "{invalid-json}");

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.name").value("fallback-product-" + suffix))
                .andExpect(jsonPath("$.data.categoryName").value("cache-fallback-category-" + suffix))
                .andExpect(jsonPath("$.data.price").value(55.5));

        String cachedJson = stringRedisTemplate.opsForValue().get(PRODUCT_DETAIL_KEY_PREFIX + productId);
        assertTrue(cachedJson != null && cachedJson.contains("fallback-product-" + suffix), "invalid cache should be replaced by database result");
    }

    @Test
    void shouldCacheHotProductsAfterFirstRead() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("hot-category-" + suffix, 1);

        Long product1Id = insertProduct(categoryId, "hot-product-1-" + suffix, new BigDecimal("11.10"), "ON_SALE");
        Long product2Id = insertProduct(categoryId, "hot-product-2-" + suffix, new BigDecimal("22.20"), "ON_SALE");
        Long product3Id = insertProduct(categoryId, "hot-product-3-" + suffix, new BigDecimal("33.30"), "ON_SALE");
        insertProduct(categoryId, "hot-product-off-sale-" + suffix, new BigDecimal("44.40"), "OFF_SALE");
        Long product4Id = insertProduct(categoryId, "hot-product-4-" + suffix, new BigDecimal("55.50"), "ON_SALE");
        Long product5Id = insertProduct(categoryId, "hot-product-5-" + suffix, new BigDecimal("66.60"), "ON_SALE");
        Long product6Id = insertProduct(categoryId, "hot-product-6-" + suffix, new BigDecimal("77.70"), "ON_SALE");

        setProductCreatedAt(product1Id, "2030-01-01 10:01:00");
        setProductCreatedAt(product2Id, "2030-01-01 10:02:00");
        setProductCreatedAt(product3Id, "2030-01-01 10:03:00");
        setProductCreatedAt(product4Id, "2030-01-01 10:04:00");
        setProductCreatedAt(product5Id, "2030-01-01 10:05:00");
        setProductCreatedAt(product6Id, "2030-01-01 10:06:00");

        mockMvc.perform(get("/products/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].name").value("hot-product-6-" + suffix))
                .andExpect(jsonPath("$.data[4].name").value("hot-product-2-" + suffix));

        String cachedJson = stringRedisTemplate.opsForValue().get(HOT_PRODUCT_KEY);
        assertTrue(cachedJson != null && cachedJson.contains("hot-product-6-" + suffix), "hot products should be written to redis");
        assertTrue(!cachedJson.contains("hot-product-off-sale-" + suffix), "off sale products should not appear in hot product cache");
    }

    @Test
    void shouldReturnHotProductsFromCacheWhenRedisIsPopulated() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("cached-hot-category-" + suffix, 1);
        Long product1Id = insertProduct(categoryId, "cached-hot-product-1-" + suffix, new BigDecimal("88.80"), "ON_SALE");
        Long product2Id = insertProduct(categoryId, "cached-hot-product-2-" + suffix, new BigDecimal("99.90"), "ON_SALE");

        setProductCreatedAt(product1Id, "2031-01-01 09:01:00");
        setProductCreatedAt(product2Id, "2031-01-01 09:02:00");

        mockMvc.perform(get("/products/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("cached-hot-product-2-" + suffix))
                .andExpect(jsonPath("$.data[0].categoryName").value("cached-hot-category-" + suffix))
                .andExpect(jsonPath("$.data[1].name").value("cached-hot-product-1-" + suffix));

        jdbcTemplate.update("delete from products where id = ?", product1Id);
        jdbcTemplate.update("delete from products where id = ?", product2Id);

        mockMvc.perform(get("/products/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("cached-hot-product-2-" + suffix))
                .andExpect(jsonPath("$.data[1].name").value("cached-hot-product-1-" + suffix));
    }

    @Test
    void shouldFallbackToDatabaseWhenCachedHotProductsJsonIsInvalid() throws Exception {
        long suffix = System.currentTimeMillis();
        Long categoryId = insertCategory("hot-fallback-category-" + suffix, 1);

        Long product1Id = insertProduct(categoryId, "hot-fallback-product-1-" + suffix, new BigDecimal("18.80"), "ON_SALE");
        Long product2Id = insertProduct(categoryId, "hot-fallback-product-2-" + suffix, new BigDecimal("28.80"), "ON_SALE");

        setProductCreatedAt(product1Id, "2032-01-01 08:01:00");
        setProductCreatedAt(product2Id, "2032-01-01 08:02:00");

        stringRedisTemplate.opsForValue().set(HOT_PRODUCT_KEY, "{invalid-json}");

        mockMvc.perform(get("/products/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("hot-fallback-product-2-" + suffix))
                .andExpect(jsonPath("$.data[1].name").value("hot-fallback-product-1-" + suffix));

        String cachedJson = stringRedisTemplate.opsForValue().get(HOT_PRODUCT_KEY);
        assertTrue(cachedJson != null && cachedJson.contains("hot-fallback-product-2-" + suffix), "invalid hot product cache should be replaced by database result");
    }

    @Test
    void shouldUpdateProductAndClearDetailCache() throws Exception {
        long suffix = System.currentTimeMillis();
        Long oldCategoryId = insertCategory("update-old-category-" + suffix, 1);
        Long newCategoryId = insertCategory("update-new-category-" + suffix, 2);
        Long productId = insertProduct(oldCategoryId, "update-old-product-" + suffix, new BigDecimal("33.30"), "ON_SALE");
        Long otherHotProductId = insertProduct(newCategoryId, "update-other-hot-product-" + suffix, new BigDecimal("66.60"), "ON_SALE");

        setProductCreatedAt(productId, "2033-01-01 07:01:00");
        setProductCreatedAt(otherHotProductId, "2033-01-01 07:02:00");

        stringRedisTemplate.opsForValue().set(
                PRODUCT_DETAIL_KEY_PREFIX + productId,
                """
                {"id":%d,"categoryId":%d,"categoryName":"stale-category","name":"stale-product","description":"stale description","price":11.1,"stock":99,"status":"ON_SALE","coverUrl":"https://example.com/stale.jpg"}
                """.formatted(productId, oldCategoryId)
        );
        stringRedisTemplate.opsForValue().set(
                HOT_PRODUCT_KEY,
                """
                [{"id":%d,"categoryId":%d,"categoryName":"stale-hot-category","name":"stale-hot-product","description":"stale hot description","price":22.2,"stock":10,"status":"ON_SALE","coverUrl":"https://example.com/stale-hot.jpg"}]
                """.formatted(productId, oldCategoryId)
        );

        mockMvc.perform(put("/products/" + productId)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "name": "update-new-product-%d",
                                  "description": "updated description",
                                  "price": 99.90,
                                  "stock": 25,
                                  "status": "OFF_SALE",
                                  "coverUrl": "https://example.com/update-new-%d.jpg"
                                }
                                """.formatted(newCategoryId, suffix, suffix)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(productId))
                .andExpect(jsonPath("$.data.categoryId").value(newCategoryId))
                .andExpect(jsonPath("$.data.categoryName").value("update-new-category-" + suffix))
                .andExpect(jsonPath("$.data.name").value("update-new-product-" + suffix))
                .andExpect(jsonPath("$.data.description").value("updated description"))
                .andExpect(jsonPath("$.data.price").value(99.9))
                .andExpect(jsonPath("$.data.stock").value(25))
                .andExpect(jsonPath("$.data.status").value("OFF_SALE"));

        String cachedAfterUpdate = stringRedisTemplate.opsForValue().get(PRODUCT_DETAIL_KEY_PREFIX + productId);
        org.junit.jupiter.api.Assertions.assertNull(cachedAfterUpdate, "product detail cache should be deleted after update");
        String hotCachedAfterUpdate = stringRedisTemplate.opsForValue().get(HOT_PRODUCT_KEY);
        org.junit.jupiter.api.Assertions.assertNull(hotCachedAfterUpdate, "hot product cache should be deleted after update");

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.categoryId").value(newCategoryId))
                .andExpect(jsonPath("$.data.categoryName").value("update-new-category-" + suffix))
                .andExpect(jsonPath("$.data.name").value("update-new-product-" + suffix))
                .andExpect(jsonPath("$.data.price").value(99.9))
                .andExpect(jsonPath("$.data.stock").value(25))
                .andExpect(jsonPath("$.data.status").value("OFF_SALE"));

        String rebuiltCache = stringRedisTemplate.opsForValue().get(PRODUCT_DETAIL_KEY_PREFIX + productId);
        assertTrue(rebuiltCache != null && rebuiltCache.contains("update-new-product-" + suffix), "product detail cache should be rebuilt with updated data");

        String hotResponse = mockMvc.perform(get("/products/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("update-other-hot-product-" + suffix))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(!hotResponse.contains("update-new-product-" + suffix), "off sale updated product should not appear in hot products");

        String rebuiltHotCache = stringRedisTemplate.opsForValue().get(HOT_PRODUCT_KEY);
        assertTrue(rebuiltHotCache != null && rebuiltHotCache.contains("update-other-hot-product-" + suffix), "hot product cache should be rebuilt with fresh data");
    }

    private void setProductCreatedAt(Long productId, String createdAt) {
        jdbcTemplate.update(
                "update products set created_at = ?, updated_at = ? where id = ?",
                createdAt,
                createdAt,
                productId
        );
    }

    @Test
    void shouldReturn404WhenUpdatingMissingProduct() throws Exception {
        mockMvc.perform(put("/products/999999999")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "name": "missing-product",
                                  "description": "missing description",
                                  "price": 88.80,
                                  "stock": 10,
                                  "status": "ON_SALE",
                                  "coverUrl": "https://example.com/missing.jpg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("product not found"));
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

    private Long insertProduct(Long categoryId, String name, BigDecimal price, String status) {
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
                    ps.setInt(5, 10);
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
}
