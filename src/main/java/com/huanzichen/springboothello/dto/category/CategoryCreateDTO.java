package com.huanzichen.springboothello.dto.category;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CategoryCreateDTO {

    @NotBlank(message = "name cannot be blank")
    private String name;

    @NotNull(message = "sort cannot be null")
    @Min(value = 0, message = "sort cannot be negative")
    private Integer sort;

    public String getName() {
        return name;
    }

    public Integer getSort() {
        return sort;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
