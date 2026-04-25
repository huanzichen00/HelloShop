package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.category.CategoryCreateDTO;
import com.huanzichen.springboothello.model.Category;
import com.huanzichen.springboothello.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/categories")
    public Result<Category> createCategory(@RequestBody @Valid CategoryCreateDTO categoryCreateDTO) {
        return Result.success(categoryService.createCategory(categoryCreateDTO));
    }

    @DeleteMapping("/categories/{id:\\d+}")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
