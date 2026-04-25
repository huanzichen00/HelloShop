package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.dto.category.CategoryCreateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
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

    public Category createCategory(CategoryCreateDTO categoryCreateDTO) {
        Category category = new Category();
        category.setName(categoryCreateDTO.getName());
        category.setSort(categoryCreateDTO.getSort());
        categoryMapper.insert(category);
        return category;
    }

    public void deleteCategory(Long id) {
        Category category = categoryMapper.findById(id);
        if (category == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "category not found");
        }

        Long productCount = categoryMapper.countProductsByCategoryId(id);
        if (productCount != null && productCount > 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "category has related products");
        }

        int rows = categoryMapper.deleteById(id);
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "category not found");
        }
    }
}
