package com.example.userservice.config;

import com.example.userservice.event.AccountCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
public class KafkaOptionalConfig {

    /**
     * Mock Kafka Template when Kafka is disabled
     * This prevents the application from failing when Kafka is not available
     */
    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, Object> kafkaTemplate() {
        log.info("Creating mock KafkaTemplate - Kafka is disabled");
        
        return new KafkaTemplate<String, Object>(null) {
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, Object data) {
                log.debug("Mock Kafka send - Topic: {}, Data: {}", topic, data);
                
                // Return a completed future to simulate successful send
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
            
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
                log.debug("Mock Kafka send - Topic: {}, Key: {}, Data: {}", topic, key, data);
                
                // Return a completed future to simulate successful send
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, Object> producerFactory() {
        log.info("Creating mock ProducerFactory - Kafka is disabled");
        return null; // Mock implementation
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedEventKafkaTemplate() {
        log.info("Creating mock AccountCreatedEvent KafkaTemplate - Kafka is disabled");
        
        return new KafkaTemplate<String, AccountCreatedEvent>(null) {
            @Override
            public CompletableFuture<SendResult<String, AccountCreatedEvent>> send(String topic, AccountCreatedEvent data) {
                log.debug("Mock Kafka send - Topic: {}, AccountCreatedEvent: {}", topic, data);
                
                // Return a completed future to simulate successful send
                CompletableFuture<SendResult<String, AccountCreatedEvent>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedEventProducerFactory() {
        log.info("Creating mock AccountCreatedEvent ProducerFactory - Kafka is disabled");
        return null; // Mock implementation
    }
}
