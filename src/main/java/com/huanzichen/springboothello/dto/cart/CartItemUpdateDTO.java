package com.huanzichen.springboothello.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemUpdateDTO {

    @NotNull(message = "quantity cannot be null")
    @Min(value = 1, message = "quantity must be greater than zero")
    private Integer quantity;

    @NotNull(message = "selected cannot be null")
    private Boolean selected;

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }
}
