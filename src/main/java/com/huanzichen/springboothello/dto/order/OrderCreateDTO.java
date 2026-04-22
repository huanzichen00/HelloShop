package com.huanzichen.springboothello.dto.order;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderCreateDTO {

    @NotEmpty(message = "cartItemIds cannot be empty")
    private List<@NotNull(message = "cartItemId cannot be empty") Long>
    cartItemIds;

    public List<Long> getCartItemIds() {
        return cartItemIds;
    }

    public void setCartItemIds(List<Long> cartItemIds) {
        this.cartItemIds = cartItemIds;
    }
}
