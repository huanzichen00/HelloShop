package com.huanzichen.springboothello.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.dto.product.ProductQueryDTO;
import com.huanzichen.springboothello.dto.product.ProductUpdateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.ProductMapper;
import com.huanzichen.springboothello.model.Product;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ProductService {

    private static final String PRODUCT_DETAIL_KEY_PREFIX = "product:detail:";
    private static final Duration PRODUCT_DETAIL_TTL = Duration.ofMinutes(30);

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductService(ProductMapper productMapper, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public PageResult<Product> searchProductsByPage(ProductQueryDTO queryDTO) {
        Integer page = queryDTO.getPage();
        Integer size = queryDTO.getSize();
        validatePageParam(page, size);

        String sort = normalizeSort(queryDTO.getSort());
        String order = normalizeOrder(queryDTO.getOrder());
        int offset = (page - 1) * size;

        List<Product> list = productMapper.searchWithConditionsAndPage(
                queryDTO.getCategoryId(),
                queryDTO.getKeyword(),
                offset,
                size,
                sort,
                order
        );
        long total = productMapper.countWithConditions(
                queryDTO.getCategoryId(),
                queryDTO.getKeyword()
        );

        int totalPages = (int) ((total + size - 1) / size);
        return new PageResult<>(total, list, page, size, totalPages);
    }

    public Product getProductById(Long id) {
        String cacheKey = PRODUCT_DETAIL_KEY_PREFIX + id;

        String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedJson != null && !cachedJson.isBlank()) {
            try {
                return objectMapper.readValue(cachedJson, Product.class);
            } catch(JsonProcessingException e) {
                stringRedisTemplate.delete(cacheKey);
            }
        }

        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "product not found");
        }

        try {
            String json = objectMapper.writeValueAsString(product);
            stringRedisTemplate.opsForValue().set(cacheKey, json, PRODUCT_DETAIL_TTL);
        } catch (JsonProcessingException e) {
        }
        return product;
    }

    public Product updateProduct(Long id, ProductUpdateDTO productUpdateDTO) {
        Product existed = productMapper.findById(id);
        if (existed == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "product not found");
        }

        Product product = new Product();
        product.setId(id);
        product.setCategoryId(productUpdateDTO.getCategoryId());
        product.setName(productUpdateDTO.getName());
        product.setDescription(productUpdateDTO.getDescription());
        product.setPrice(productUpdateDTO.getPrice());
        product.setStock(productUpdateDTO.getStock());
        product.setStatus(productUpdateDTO.getStatus());
        product.setCoverUrl(productUpdateDTO.getCoverUrl());

        int rows = productMapper.updateById(product);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to update product");
        }

        stringRedisTemplate.delete(PRODUCT_DETAIL_KEY_PREFIX + id);
        return productMapper.findById(id);
    }

    private void validatePageParam(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "page must be greater than zero");
        }
        if (size == null || size <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "size must be greater than zero");
        }
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "id";
        }
        if (!"id".equals(sort) && !"price".equals(sort)
        && !"createdAt".equals(sort)) {
            return "id";
        }
        return sort;
    }

    private String normalizeOrder(String order) {
        if (order == null || order.trim().isEmpty()) {
            return "asc";
        }
        order = order.toLowerCase();
        if (!"asc".equals(order) && !"desc".equals(order)) {
            return "asc";
        }
        return order;
    }
}
