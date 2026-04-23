package com.huanzichen.springboothello.service;

import com.huanzichen.springboothello.config.RabbitMqConfig;
import com.huanzichen.springboothello.dto.order.OrderCreatedMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public OrderMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderCreatedMessage(OrderCreatedMessage message) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.ORDER_EXCHANGE,
                RabbitMqConfig.ORDER_CREATED_ROUTING_KEY, message
        );
    }
}
