package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.product.ProductCreateDTO;
import com.huanzichen.springboothello.dto.product.ProductQueryDTO;
import com.huanzichen.springboothello.dto.product.ProductUpdateDTO;
import com.huanzichen.springboothello.model.Product;
import com.huanzichen.springboothello.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/products")
    public Result<Product> createProduct(@RequestBody @Valid ProductCreateDTO productCreateDTO) {
        return Result.success(productService.createProduct(productCreateDTO));
    }

    @GetMapping("/products/{id:\\d+}")
    public Result<Product> getProductById(@PathVariable Long id) {
        return Result.success(productService.getProductById(id));
    }

    @GetMapping("/products/hot")
    public Result<List<Product>> listHotProducts() {
        return Result.success(productService.listHotProducts());
    }

    @PutMapping("/products/{id:\\d+}")
    public Result<Product> updateProductById(@PathVariable Long id, @RequestBody @Valid ProductUpdateDTO productUpdateDTO) {
        return Result.success(productService.updateProduct(id, productUpdateDTO));
    }

    @DeleteMapping("/products/{id:\\d+}")
    public Result<Void> deleteProductById(@PathVariable Long id) {
        productService.deleteProduct(id);
        return Result.success();
    }
}
