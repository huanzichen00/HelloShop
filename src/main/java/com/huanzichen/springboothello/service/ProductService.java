package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.dto.product.ProductQueryDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.ProductMapper;
import com.huanzichen.springboothello.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductMapper productMapper;

    public ProductService(ProductMapper productMapper) {
        this.productMapper = productMapper;
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
        Product product = productMapper.findById(id);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "product not found");
        }
        return product;
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
