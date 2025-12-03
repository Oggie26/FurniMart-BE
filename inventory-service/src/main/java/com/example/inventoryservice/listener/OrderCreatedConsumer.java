package com.example.inventoryservice.listener;
import com.example.inventoryservice.entity.ProcessedMessage;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.event.OrderCreatedEvent;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.StoreClient;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.ProcessedMessageRepository;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.StoreResponse;
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
    private final InventoryRepository inventoryRepository;
    private final StoreClient storeClient;
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

        // Kiểm tra xem order đã được xử lý chưa (idempotency check)
        if (processedMessageRepository.existsByOrderId(orderId)) {
            log.warn("Order {} has already been processed. Skipping duplicate processing.", orderId);
            return;
        }

        try {
            // Xử lý các items trong order
            event.getItems().forEach(item -> {
                log.info("Processing productColorId={} quantity={}", item.getProductColorId(), item.getQuantity());
                try {
//                Inventory inventory = inventoryRepository.findByProductColorId(item.getProductColorId())
//                        .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cho productColorId=" + item.getProductColorId()));
//                String warehouseId = getStoreById(event.getStoreId());
//                inventoryService.decreaseStock(
//                        item.getProductColorId(),
//                        inventory.getLocationItem().getId(),
//                        item.getQuantity(),
//                        warehouseId
//                );
                    inventoryService.reserveStock(item.getProductColorId(), item.getQuantity(), event.getOrderId());

                    log.info("Reserved stock for productColorId={} by {}", item.getProductColorId(), item.getQuantity());
                } catch (Exception e) {
                    log.error("Error reserving stock for productColorId={} : {}", item.getProductColorId(), e.getMessage(), e);
                    throw new RuntimeException("Failed to reserve stock for productColorId: " + item.getProductColorId(), e);
                }
            });

            // Lưu ProcessedMessage sau khi xử lý thành công
            ProcessedMessage processedMessage = ProcessedMessage.builder()
                    .orderId(orderId)
                    .build();
            processedMessageRepository.save(processedMessage);
            
            log.info("Successfully processed order {} and saved processed message record", orderId);
        } catch (Exception e) {
            log.error("Error processing order {}: {}", orderId, e.getMessage(), e);
            // Không lưu ProcessedMessage nếu xử lý thất bại, để có thể retry sau
            throw e;
        }
    }

    private String getStoreById(String storeId) {
        ApiResponse<StoreResponse> response = storeClient.getStoreById(storeId);
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }
        return response.getData().getId();
    }
}
