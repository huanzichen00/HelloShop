package com.huanzichen.springboothello.consumer;

import com.huanzichen.springboothello.config.RabbitMqConfig;
import com.huanzichen.springboothello.dto.order.OrderCreatedMessage;
import com.huanzichen.springboothello.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    private final NotificationService notificationService;

    public OrderCreatedConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedMessage message) {
        notificationService.createOrderCreatedNotification(
                message.getUserId(),
                message.getOrderId(),
                message.getOrderNo()
        );
    }
}
