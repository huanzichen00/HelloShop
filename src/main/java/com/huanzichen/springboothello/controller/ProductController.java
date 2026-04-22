package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.product.ProductQueryDTO;
import com.huanzichen.springboothello.model.Product;
import com.huanzichen.springboothello.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProductController {

    private final ProductService productService;
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public Result<PageResult<Product>> searchProductsByPage(ProductQueryDTO queryDTO) {
        return Result.success(productService.searchProductsByPage(queryDTO));
    }

    @GetMapping("/products/{id:\\d+}")
    public Result<Product> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }
}
