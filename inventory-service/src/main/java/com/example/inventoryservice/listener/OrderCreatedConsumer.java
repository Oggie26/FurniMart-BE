package com.example.inventoryservice.listener;

import com.example.inventoryservice.event.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderCreatedConsumer {

//    private final RedisTemplate<String, Object> redisTemplate;
//
//    public OrderCreatedConsumer(RedisTemplate<String, Object> redisTemplate) {
//        this.redisTemplate = redisTemplate;
//    }

    @KafkaListener(topics = "order-created-topic", groupId = "inventory-group")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📦 Received OrderCreatedEvent for order: {}", event.getOrderId());

        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            String key = "reserved_stock:" + item.getProductColorId();
//            redisTemplate.opsForValue().increment(key, item.getQuantity());
        }
    }
}

