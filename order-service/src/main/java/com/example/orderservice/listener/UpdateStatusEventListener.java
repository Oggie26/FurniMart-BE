//package com.example.orderservice.listener;
//
//import com.example.orderservice.event.UpdateStatusOrderCreatedEvent;
//import com.example.orderservice.service.inteface.OrderService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Lazy;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class UpdateStatusEventListener {
//
//    @Lazy
//    private final OrderService orderService;
//
//    @KafkaListener(topics = "update-status-order-created-topic",
//            groupId = "inventory-group",
//            containerFactory = "updateStatusCreatedKafkaListenerContainerFactory"
//    )
//    public void handleUpdateStatusOrderCreated(UpdateStatusOrderCreatedEvent event) {
//        try {
//            orderService.updateOrderStatus(event.getOrderId(), event.getEnumProcessOrder());
//            log.info("✔ Order {} status updated to {}", event.getOrderId(), event.getEnumProcessOrder());
//        } catch (Exception ex) {
//            log.error("Failed to update order {} status: {}", event.getOrderId(), ex.getMessage(), ex);
//        }
//    }
//}
//
//
//
//
//
//
