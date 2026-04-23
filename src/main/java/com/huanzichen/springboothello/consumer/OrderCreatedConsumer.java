package com.huanzichen.springboothello.consumer;

import com.huanzichen.springboothello.config.RabbitMqConfig;
import com.huanzichen.springboothello.dto.order.OrderCreatedMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderCreatedConsumer {

    @RabbitListener(queues = RabbitMqConfig.ORDER_CREATED_QUEUE)
    public void handleOrderCreated(OrderCreatedMessage message) {
        System.out.println("received order created message, "
                + "orderId = " + message.getOrderId()
                + ", orderNo = " + message.getOrderNo()
                + ", totalAmount = " + message.getTotalAmount()
        );
    }
}
