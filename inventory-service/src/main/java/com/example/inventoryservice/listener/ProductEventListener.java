package com.example.inventoryservice.listener;

import com.example.inventoryservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    @KafkaListener(topics = "product-updated-topic", groupId = "inventory-service-group")
    public void handleProductUpdate(ProductUpdatedEvent event) {
        log.info("Received product update event for id: {}", event.getProductId());

        // Cache eviction removed since Redis was removed from inventory-service
        // Product data will be fetched fresh from product-service via Feign client
        log.debug("Product update event processed. No cache to evict.");
    }
}
