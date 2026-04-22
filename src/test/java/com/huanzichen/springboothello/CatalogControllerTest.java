package com.huanzichen.springboothello;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> productIds = new ArrayList<>();
    private final List<Long> categoryIds = new ArrayList<>();

    @AfterEach
    void cleanup() {
        for (Long productId : productIds) {
            jdbcTemplate.update("delete from products where id = ?", productId);
        }
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
