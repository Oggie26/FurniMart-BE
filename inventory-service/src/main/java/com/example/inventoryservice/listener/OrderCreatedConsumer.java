package com.example.inventoryservice.listener;

import com.example.inventoryservice.entity.ProcessedMessage;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.repository.ProcessedMessageRepository;
import com.example.inventoryservice.response.ReserveStockResponse;
import com.example.inventoryservice.service.InventoryServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryServiceImpl inventoryService;
    private final ProcessedMessageRepository processedMessageRepository;

    @KafkaListener(
            topics = "order-created-topic",
            groupId = "inventory-group",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        Long orderId = event.getOrderId();
        log.info("Received OrderCreatedEvent for order: {}", orderId);

        if (processedMessageRepository.existsByOrderId(orderId)) {
            return;
        }

        try {
            event.getItems().forEach(item -> {
                log.info("🔹 Processing reserve for productColorId={} quantity={}", item.getProductColorId(), item.getQuantity());
                try {
                    ReserveStockResponse response = inventoryService.reserveStock(
                            item.getProductColorId(),
                            item.getQuantity(),
                            event.getOrderId()
                    );

                    if (response.getInventory() != null) {
                        log.info("✅ Created Reserve Ticket: {}. Reserved: {}/{}. Missing: {}",
                                response.getInventory().getCode(),
                                response.getQuantityReserved(),
                                item.getQuantity(),
                                response.getQuantityMissing());
                    } else {
                        log.warn("⚠️ Out of Stock locally. Reserved: 0/{}", item.getQuantity());
                    }
//                    InventoryReservedEvent successEvent = new InventoryReservedEvent(orderId);
//                    kafkaTemplate.send("inventory-reserved-topic", successEvent);
                } catch (Exception e) {
                    log.error("❌ Error reserving stock for item {}: {}", item.getProductColorId(), e.getMessage());
                    throw new RuntimeException("Failed to process item: " + item.getProductColorId(), e);
                }
            });

            ProcessedMessage processedMessage = ProcessedMessage.builder()
                    .orderId(orderId)
                    .build();
            processedMessageRepository.save(processedMessage);

            log.info("✅ Successfully processed order {}", orderId);

        } catch (Exception e) {
            log.error("❌ Error processing order {}: {}", orderId, e.getMessage(), e);
            throw e; // Throw để Kafka retry
        }
    }
}