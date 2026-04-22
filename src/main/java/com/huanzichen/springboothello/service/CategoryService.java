package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.mapper.CategoryMapper;
import com.huanzichen.springboothello.model.Category;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryMapper categoryMapper) {
        this.categoryMapper = categoryMapper;
    }

    public List<Category> listCategories() {
        return categoryMapper.findAll();
    }
}
