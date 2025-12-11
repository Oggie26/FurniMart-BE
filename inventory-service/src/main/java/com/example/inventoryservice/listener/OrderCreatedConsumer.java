package com.example.inventoryservice.listener;

import com.example.inventoryservice.entity.ProcessedMessage;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.repository.ProcessedMessageRepository;
import com.example.inventoryservice.response.ReserveStockResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class OrderCreatedConsumer {
//
//    private final InventoryServiceImpl inventoryService;
//    private final ProcessedMessageRepository processedMessageRepository;
//
//    @KafkaListener(
//            topics = "order-created-topic",
//            groupId = "inventory-group",
//            containerFactory = "orderCreatedKafkaListenerContainerFactory"
//    )
//    public void handleOrderCreated(OrderCreatedEvent event) {
//        Long orderId = event.getOrderId();
//        log.info("Received OrderCreatedEvent for order: {}", orderId);
//
//        if (processedMessageRepository.existsByOrderId(orderId)) {
//            return;
//        }
//
//        try {
//            event.getItems().forEach(item -> {
//                log.info("🔹 Processing reserve for productColorId={} quantity={}", item.getProductColorId(), item.getQuantity());
//                try {
//                    ReserveStockResponse response = inventoryService.reserveStock(
//                            item.getProductColorId(),
//                            item.getQuantity(),
//                            event.getOrderId()
//                    );
//
//                    if (response.getReservations() != null) {
//                        log.info("✅ Created Reserve Ticket: {}. Reserved: {}/{}. Missing: {}",
//                                response.getReservations(),
//                                response.getQuantityReserved(),
//                                item.getQuantity(),
//                                response.getQuantityMissing());
//                    } else {
//                        log.warn("⚠️ Out of Stock locally. Reserved: 0/{}", item.getQuantity());
//                    }
//                } catch (Exception e) {
//                    log.error("❌ Error reserving stock for item {}: {}", item.getProductColorId(), e.getMessage());
//                    throw new RuntimeException("Failed to process item: " + item.getProductColorId(), e);
//                }
//            });
//
//            ProcessedMessage processedMessage = ProcessedMessage.builder()
//                    .orderId(orderId)
//                    .build();
//            processedMessageRepository.save(processedMessage);
//
//            log.info("✅ Successfully processed order {}", orderId);
//
//        } catch (Exception e) {
//            log.error("❌ Error processing order {}: {}", orderId, e.getMessage(), e);
//            throw e;
//        }
//    }
//}



@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryService inventoryService;
    private final ProcessedMessageRepository processedMessageRepository;

    @KafkaListener(
            topics = "order-created-topic",
            groupId = "inventory-group",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        Long orderId = event.getOrderId();
        log.info("📥 Received OrderCreatedEvent for order: {}", orderId);

        // 1. Idempotency check
        if (processedMessageRepository.existsByOrderId(orderId)) {
            log.warn("⚠️ Order {} already processed. Skipping.", orderId);
            return;
        }

        try {
            // 2. Reserve stock cho từng item
            event.getItems().forEach(item -> {
                log.info("🔹 Reserving: productColorId={} qty={}",
                        item.getProductColorId(), item.getQuantity());

                ReserveStockResponse response =
                        inventoryService.reserveStock(
                                item.getProductColorId(),
                                item.getQuantity(),
                                orderId
                        );

                log.info("✅ Reserved: {}/{} for productColorId={}",
                        response.getTotalReserved(),
                        item.getQuantity(),
                        item.getProductColorId());
            });

            // 3. Lưu lại processed message
            processedMessageRepository.save(
                    ProcessedMessage.builder()
                            .orderId(orderId)
                            .build()
            );

            log.info("✅ Successfully processed order {}", orderId);

        } catch (Exception e) {
            log.error("❌ Failed to process order {}: {}", orderId, e.getMessage(), e);
            throw e; // ❗ Cho Kafka retry
        }
    }
}
