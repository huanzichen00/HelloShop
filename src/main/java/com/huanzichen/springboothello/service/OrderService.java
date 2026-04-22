package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.common.ErrorCode;
import com.huanzichen.springboothello.common.UserContext;
import com.huanzichen.springboothello.dto.order.OrderCreateDTO;
import com.huanzichen.springboothello.exception.BusinessException;
import com.huanzichen.springboothello.mapper.CartItemMapper;
import com.huanzichen.springboothello.mapper.OrderItemMapper;
import com.huanzichen.springboothello.mapper.OrderMapper;
import com.huanzichen.springboothello.mapper.ProductMapper;
import com.huanzichen.springboothello.model.CartItem;
import com.huanzichen.springboothello.model.Order;
import com.huanzichen.springboothello.model.OrderItem;
import com.huanzichen.springboothello.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {
    private final CartItemMapper cartItemMapper;
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final OrderItemMapper orderItemMapper;

    public OrderService(CartItemMapper cartItemMapper, OrderMapper orderMapper, ProductMapper productMapper, OrderItemMapper orderItemMapper) {
        this.cartItemMapper = cartItemMapper;
        this.orderMapper = orderMapper;
        this.productMapper = productMapper;
        this.orderItemMapper = orderItemMapper;
    }

    @Transactional
    public Order createOrder(OrderCreateDTO orderCreateDTO) {
        Long userId = UserContext.getCurrentUserId();

        List<CartItem> cartItems = getOwnedCartItems(userId, orderCreateDTO.getCartItemIds());

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQuantity = 0;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = getAvailableProduct(cartItem.getProductId());

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "insufficient stock");
            }

            BigDecimal subtotalAmount = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductPrice(product.getPrice());
            orderItem.setProductCoverUrl(product.getCoverUrl());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotalAmount(subtotalAmount);

            orderItems.add(orderItem);

            totalAmount = totalAmount.add(subtotalAmount);
            totalQuantity += cartItem.getQuantity();
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setOrderNo(generateOrderNo());
        order.setStatus("PENDING_PAYMENT");
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQuantity);

        orderMapper.insert(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(order.getId());
            orderItemMapper.insert(orderItem);
        }

        deductStock(cartItems);
        removeCartItems(orderCreateDTO.getCartItemIds());

        Order createdOrder = orderMapper.findById(order.getId());
        createdOrder.setItems(orderItemMapper.findByOrderId(order.getId()));
        return createdOrder;
    }

    public List<Order> listMyOrders() {
        Long userId = UserContext.getCurrentUserId();
        return orderMapper.findByUserId(userId);
    }

    public Order getOrderDetail(Long id) {
        Long userId = UserContext.getCurrentUserId();
        Order order = orderMapper.findByIdAndUserId(id, userId);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "order not found");
        }
        order.setItems(orderItemMapper.findByOrderId(id));
        return order;
    }

    @Transactional
    public Order cancelOrder(Long id) {
        Long userId = UserContext.getCurrentUserId();

        Order order = orderMapper.findByIdAndUserId(id, userId);
        validateCancelableOrder(order);

        List<OrderItem> orderItems = orderItemMapper.findByOrderId(id);

        int rows = orderMapper.updateStatusById(id, "CANCELED");
        if (rows == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to cancel order");
        }

        restoreStock(orderItems);

        Order canceledOrder = orderMapper.findByIdAndUserId(id, userId);
        canceledOrder.setItems(orderItemMapper.findByOrderId(id));
        return canceledOrder;
    }

    @Transactional
    public Order payOrder(Long id) {
        Long userId = UserContext.getCurrentUserId();

        Order order = orderMapper.findByIdAndUserId(id, userId);
        validatePayableOrder(order);

        int rows = orderMapper.updateStatusById(id, "PAID");
        if (rows == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to pay order");
        }

        Order paidOrder = orderMapper.findByIdAndUserId(id, userId);
        paidOrder.setItems(orderItemMapper.findByOrderId(id));
        return paidOrder;
    }

    private static void validateCancelableOrder(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "order not found");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "order cannot be canceled");
        }
    }

    private static void validatePayableOrder(Order order) {
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "order not found");
        }
        if (!"PENDING_PAYMENT".equals(order.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "order cannot be payed");
        }
    }

    private List<CartItem> getOwnedCartItems(Long userId, List<Long> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cart items cannot be empty");
        }

        Set<Long> uniqueIds = new HashSet<>(cartItemIds);
        if (uniqueIds.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "duplicate cart items ids");
        }

        List<CartItem> cartItems = cartItemMapper.findByIds(cartItemIds);
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "cart items cannot be empty");
        }
        if (cartItems.size() != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "some cart items do not exist");
        }

        for (CartItem cartItem : cartItems) {
            if (!userId.equals(cartItem.getUserId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "no permission");
            }
            if (!Boolean.TRUE.equals(cartItem.getSelected())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "cart item is not selected");
            }
        }
        return cartItems;
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

    private String generateOrderNo() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void deductStock(List<CartItem> cartItems) {
        for (CartItem cartItem : cartItems) {
            int rows = productMapper.deductStock(cartItem.getProductId(), cartItem.getQuantity());
            if (rows == 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "insufficient stock");
            }
        }
    }

    private void removeCartItems(List<Long> cartItemIds) {
        int rows = cartItemMapper.deleteByIds(cartItemIds);
        if (rows != cartItemIds.size()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to remove cart items");
        }
    }

    private void restoreStock(List<OrderItem> orderItems ) {
        for (OrderItem orderItem : orderItems) {
            if (orderItem.getProductId() != null) {
                int rows = productMapper.restoreStock(orderItem.getProductId(), orderItem.getQuantity());
                if (rows == 0) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "failed to restore stock");
                }
            }
        }
    }
}
