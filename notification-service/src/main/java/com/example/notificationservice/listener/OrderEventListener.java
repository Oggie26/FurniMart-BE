package com.example.notificationservice.listener;

import com.example.notificationservice.enums.ErrorCode;
import com.example.notificationservice.event.OrderCancelledEvent;
import com.example.notificationservice.event.OrderCreatedEvent;
import com.example.notificationservice.event.OrderDeliveredEvent;
import com.example.notificationservice.event.DeliveryAssignedEvent;
import com.example.notificationservice.exception.AppException;
import com.example.notificationservice.feign.OrderClient;
import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.response.OrderResponse;
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
    private final OrderClient orderClient;

    @KafkaListener(topics = "order-created-topic", groupId = "notification-group", containerFactory = "orderCreatedKafkaListenerContainerFactory")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📦 Received OrderCreatedEvent for order: {}", event.getOrderId());

        OrderResponse order = getOrderResponse(event.getOrderId());

        if (order == null) {
            log.warn("Order with ID {} not found. Skipping notification.", event.getOrderId());
            return;
        }
        orderService.sendMailToCreateOrderSuccess(event);
        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            String key = "reserved_stock:" + item.getProductColorId();
            log.info("Reserved stock key: {}", key);
        }
    }

    @KafkaListener(topics = "order-cancelled-topic", groupId = "notification-group", containerFactory = "orderCancelledKafkaListenerContainerFactory")
    public void handleCancelOrderCreated(OrderCancelledEvent event) {
        log.info("📦 Received OrderCancelledEvent for order: {}", event.getOrderId());
        orderService.sendMailToCancelOrder(event);
    }

    @KafkaListener(topics = "store-assigned-topic", groupId = "notification-group", containerFactory = "orderCreatedKafkaListenerContainerFactory")
    public void handleAssignedOrderCreated(OrderCreatedEvent event) {
        OrderResponse order = getOrderResponse(event.getOrderId());

        if (order == null) {
            log.warn("Order with ID {} not found. Skipping notification.", event.getOrderId());
            return;
        }
        orderService.sendMailToStoreAssigned(event);
        for (OrderCreatedEvent.OrderItem item : event.getItems()) {
            String key = "reserved_stock:" + item.getProductColorId();
            log.info("Reserved stock key: {}", key);
        }
    }

    @KafkaListener(topics = "order-delivered-topic", groupId = "notification-group", containerFactory = "orderDeliveredKafkaListenerContainerFactory")
    public void handleOrderDelivered(OrderDeliveredEvent event) {
        log.info("📦 Received OrderDeliveredEvent for order: {}", event.getOrderId());
        orderService.sendMailToOrderDelivered(event);
    }

    @KafkaListener(topics = "delivery-assigned-topic", groupId = "notification-group", containerFactory = "deliveryAssignedKafkaListenerContainerFactory")
    public void handleDeliveryAssigned(DeliveryAssignedEvent event) {
        log.info("🚚 Received DeliveryAssignedEvent for order: {}", event.getOrderId());
        orderService.sendMailToDeliveryAssigned(event);
    }

    private OrderResponse getOrderResponse(long orderId) {
        try {
            ApiResponse<OrderResponse> response = orderClient.getOrderById(orderId);
            if (response != null && response.getData() != null) {
                return response.getData();
            } else {
                log.warn("OrderClient returned null for orderId {}", orderId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error fetching order with id {}: {}", orderId, e.getMessage(), e);
            throw new AppException(ErrorCode.NOT_FOUND_ORDER);
        }
    }
}
