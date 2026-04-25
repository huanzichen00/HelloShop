package com.huanzichen.springboothello.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ProductCreateDTO {

    @NotNull(message = "categoryId cannot be null")
    private Long categoryId;

    @NotBlank(message = "name cannot be blank")
    private String name;

    private String description;

    @NotNull(message = "price cannot be null")
    @DecimalMin(value = "0.01", message = "price must be greater than zero")
    private BigDecimal price;

    @NotNull(message = "stock cannot be null")
    @Min(value = 0, message = "stock cannot be negative")
    private Integer stock;

    @NotBlank(message = "status cannot be blank")
    private String status;

    private String coverUrl;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}
