package com.example.inventoryservice.listener;
import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.service.InventoryServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final InventoryServiceImpl inventoryService;
    private final InventoryRepository inventoryRepository;

    @KafkaListener(
            topics = "order-created-topic",
            groupId = "inventory-group",
            containerFactory = "inventoryCreatedKafkaListenerContainerFactory"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📦 Received OrderCreatedEvent for order: {}", event.getOrderId());

        event.getItems().forEach(item -> {
            log.info("🔹 Processing productColorId={} quantity={}", item.getProductColorId(), item.getQuantity());
            try {
                Inventory inventory = inventoryRepository.findByProductColorId(item.getProductColorId())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cho productColorId=" + item.getProductColorId()));

                inventoryService.decreaseStock(
                        item.getProductColorId(),
                        inventory.getLocationItem().getId(),
                        item.getQuantity()
                );

                log.info("✅ Decreased stock for productColorId={} by {}", item.getProductColorId(), item.getQuantity());
            } catch (Exception e) {
                log.error("❌ Error decreasing stock for productColorId={} : {}", item.getProductColorId(), e.getMessage(), e);
            }
        });
    }
}
