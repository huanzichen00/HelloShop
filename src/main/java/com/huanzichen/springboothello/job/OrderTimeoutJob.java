package com.huanzichen.springboothello.job;

import com.huanzichen.springboothello.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class OrderTimeoutJob {

    private static final Logger log = LoggerFactory.getLogger(OrderTimeoutJob.class);

    private final OrderService orderService;

    @Value("${order.timeout-minutes:30}")
    private long orderTimeoutMinutes;

    public OrderTimeoutJob(OrderService orderService) {
        this.orderService = orderService;
    }

    // 每分钟执行一次
    @Scheduled(fixedRate = 60000)
    public void cancelTimeoutOrders() {
        // 取消超过配置时长仍未支付的订单
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderTimeoutMinutes);
        int canceledCount = orderService.cancelTimeoutOrders(deadline);
        log.info("order timeout job finished, timeoutMinutes={}, canceledCount={}",
                orderTimeoutMinutes, canceledCount);
    }
}
