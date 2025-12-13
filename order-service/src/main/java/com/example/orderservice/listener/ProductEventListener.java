package com.example.orderservice.listener;

import com.example.orderservice.event.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductEventListener {

    private final CacheManager cacheManager;

    @KafkaListener(topics = "product-updated-topic", groupId = "order-service-group")
    public void handleProductUpdate(ProductUpdatedEvent event) {
        log.info("Received product update event for id: {}", event.getProductId());

        // 1. Evict "products" cache by key
        if (event.getProductId() != null) {
            try {
                Objects.requireNonNull(cacheManager.getCache("products")).evict(event.getProductId());
                log.info("Evicted product cache for id: {}", event.getProductId());
            } catch (Exception e) {
                log.warn("Failed to evict products cache: {}", e.getMessage());
            }
        }

        // 2. Clear "product-colors" cache
        // Since we don't map Product -> list of ProductColor IDs here easily,
        // we clear all to ensure consistency.
        // (Improving this would require Product Service to send list of affected Color
        // IDs in the event)
        try {
            Objects.requireNonNull(cacheManager.getCache("product-colors")).clear();
            log.info("Cleared all product-colors cache");
        } catch (Exception e) {
            log.warn("Failed to clear product-colors cache: {}", e.getMessage());
        }
    }
}
