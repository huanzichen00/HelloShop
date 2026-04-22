package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.model.Category;
import com.huanzichen.springboothello.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/categories")
    public Result<List<Category>> listCategories() {
        return Result.success(categoryService.listCategories());
    }
}
