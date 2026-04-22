package com.huanzichen.springboothello.mapper;

import com.huanzichen.springboothello.model.CartItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CartItemMapper {

    int insert(CartItem cartItem);

    int update(CartItem cartItem);

    int deleteById(Long id);

    CartItem findById(Long id);

    CartItem findByUserIdAndProductId(@Param("userId") Long userId,
                                      @Param("productId") Long productId);

    CartItem findDetailById(Long id);

    List<CartItem> findDetailsByUserId(Long userId);

    List<CartItem> findByIds(@Param("ids") List<Long> ids);

    int deleteByIds(@Param("ids") List<Long> ids);
}
