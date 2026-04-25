package com.huanzichen.springboothello.controller;

import com.huanzichen.springboothello.common.PageResult;
import com.huanzichen.springboothello.common.Result;
import com.huanzichen.springboothello.dto.order.OrderCreateDTO;
import com.huanzichen.springboothello.model.Order;
import com.huanzichen.springboothello.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<Order> createOrder(@RequestBody @Valid OrderCreateDTO orderCreateDTO) {
        return Result.success(orderService.createOrder(orderCreateDTO));
    }

    @GetMapping
    public Result<List<Order>> listMyOrders() {
        return Result.success(orderService.listMyOrders());
    }

    @GetMapping("/page")
    public Result<PageResult<Order>> listMyOrdersByPage(Integer page,
                                                        Integer size,
                                                        String status,
                                                        String sort,
                                                        String order) {
        return Result.success(orderService.listMyOrdersByPage(page, size, status, sort, order));
    }

    @GetMapping("/{id}")
    public Result<Order> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getOrderDetail(id));
    }

    @PutMapping("/{id}/cancel")
    public Result<Order> cancelOrder(@PathVariable Long id) {
        return Result.success(orderService.cancelOrder(id));
    }

    @PutMapping("/{id}/pay")
    public Result<Order> payOrder(@PathVariable Long id) {
        return Result.success(orderService.payOrder(id));
    }

    @PutMapping("/{id}/complete")
    public Result<Order> completeOrder(@PathVariable Long id) {
        return Result.success(orderService.completeOrder(id));
    }
}
