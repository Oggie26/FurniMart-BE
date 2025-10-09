package com.example.notificationservice.listener;

import com.example.notificationservice.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventListener {

    @KafkaListener(topics = "order-created-topic", groupId = "notification-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📦 Received OrderCreatedEvent for order: {}", event.getOrderId());

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            String key = "reserved_stock:" + item.getProductColorId();
//            redisTemplate.opsForValue().increment(key, item.getQuantity());
        }
    }
}
