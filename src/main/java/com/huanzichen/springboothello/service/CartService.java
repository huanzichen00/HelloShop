package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.dto.cart.CartItemAddDTO;
import com.huanzichen.springboothello.dto.cart.CartItemUpdateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.CartItemMapper;
import com.huanzichen.springboothello.mapper.ProductMapper;
import com.huanzichen.springboothello.model.CartItem;
import com.huanzichen.springboothello.model.Product;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public CartService(CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartItemMapper = cartItemMapper;
        this.productMapper =  productMapper;
    }

    public List<CartItem> listCartItems() {
        Long userId = UserContext.getCurrentUserId();
        return cartItemMapper.findDetailsByUserId(userId);
    }

    public CartItem addCartItem(CartItemAddDTO cartItemAddDTO) {
        Long userId = UserContext.getCurrentUserId();

        Product product = getAvailableProduct(cartItemAddDTO.getProductId());
        validateStock(product, cartItemAddDTO.getQuantity());

        CartItem existedItem = cartItemMapper.findByUserIdAndProductId(userId, cartItemAddDTO.getProductId());
        if (existedItem != null) {
            int newQuantity = existedItem.getQuantity() + cartItemAddDTO.getQuantity();
            validateStock(product, newQuantity);

            existedItem.setQuantity(newQuantity);
            existedItem.setSelected(true);
            cartItemMapper.update(existedItem);

            return cartItemMapper.findDetailById(existedItem.getId());
        }
        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProductId(product.getId());
        cartItem.setQuantity(cartItemAddDTO.getQuantity());
        cartItem.setSelected(true);

        cartItemMapper.insert(cartItem);
        return cartItemMapper.findDetailById(cartItem.getId());
    }

    public CartItem updateCartItem (Long id, CartItemUpdateDTO cartItemUpdateDTO) {
        CartItem ownedItem = getOwnedCartItem(id);

        Product product = getAvailableProduct(ownedItem.getProductId());
        validateStock(product, cartItemUpdateDTO.getQuantity());

        ownedItem.setQuantity(cartItemUpdateDTO.getQuantity());
        ownedItem.setSelected(cartItemUpdateDTO.getSelected());

        cartItemMapper.update(ownedItem);
        return cartItemMapper.findDetailById(id);
    }

    public void deleteCartItem(Long id) {
        CartItem ownedItem = getOwnedCartItem(id);

        int rows = cartItemMapper.deleteById(ownedItem.getId());
        if (rows == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "cart item not found");
        }
    }

    private CartItem getOwnedCartItem(Long id) {
        CartItem cartItem = cartItemMapper.findById(id);
        if (cartItem == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "cartItem not found");
        }

        Long currentUserId = UserContext.getCurrentUserId();
        if (!currentUserId.equals(cartItem.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "no permission");
        }

        return cartItem;
    }

    private Product getAvailableProduct(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "product not found");
        }
        if (!"ON_SALE".equals(product.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "product is not on sale");
        }
        return product;
    }

    private void validateStock(Product product, Integer quantity) {
        if (quantity > product.getStock()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "insuffcient stock");
        }
    }
}
