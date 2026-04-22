package com.huanzichen.springboothello.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemAddDTO {

    @NotNull(message = "productId cannot be null")
    private Long productId;

    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be greater than zero")
    private Integer quantity;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
