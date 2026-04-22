package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.cart.CartItemAddDTO;
import com.huanzichen.springboothello.dto.cart.CartItemUpdateDTO;
import com.huanzichen.springboothello.model.CartItem;
import com.huanzichen.springboothello.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    // GET
    @GetMapping
    public Result<List<CartItem>> getCartItems() {
        return Result.success(cartService.listCartItems());
    }

    // POST    /items
    @PostMapping("/items")
    public Result<CartItem> addCartItem(@RequestBody @Valid CartItemAddDTO cartItemAddDTO) {
        return Result.success(cartService.addCartItem(cartItemAddDTO));
    }

    // PUT     /items/{id}
    @PutMapping("/items/{id}")
    public Result<CartItem> updateCartItem(@RequestBody @Valid CartItemUpdateDTO cartItemUpdateDTO, @PathVariable Long id) {
        return Result.success(cartService.updateCartItem(id, cartItemUpdateDTO));
    }

    // DELETE  /items/{id}
    @DeleteMapping("/items/{id}")
    public Result<Void> deleteCartItem(@PathVariable Long id) {
        cartService.deleteCartItem(id);
        return Result.success();
    }
}
