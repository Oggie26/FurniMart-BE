package com.example.notificationservice.listener;

import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.service.EmailOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {
    private final EmailOrderService orderService;

    @KafkaListener(topics = "order-created-topic",
            groupId = "notification-group",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for order: {}", event.getOrderId());
        orderService.sendMailToCreateOrderSuccess(event);
        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            String key = "reserved_stock:" + item.getProductColorId();
        }
    }
}
