package com.example.userservice.config;

import com.example.userservice.event.AccountCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.Producer;
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

    // Mock ProducerFactory for tests when Kafka is disabled
    private <K, V> ProducerFactory<K, V> createMockProducerFactory() {
        return new ProducerFactory<K, V>() {
            @Override
            public Producer<K, V> createProducer() {
                return null; // Not used in tests
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, Object> producerFactory() {
        return createMockProducerFactory();
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, Object> kafkaTemplate() {
        ProducerFactory<String, Object> factory = createMockProducerFactory();
        return new KafkaTemplate<String, Object>(factory) {
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, Object data) {
                log.warn("Kafka is disabled. Message to topic '{}' will NOT be sent. Data: {}", topic, data);
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
            
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
                log.warn("Kafka is disabled. Message to topic '{}' with key '{}' will NOT be sent. Data: {}", topic, key, data);
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedEventProducerFactory() {
        return createMockProducerFactory();
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedEventKafkaTemplate() {
        ProducerFactory<String, AccountCreatedEvent> factory = createMockProducerFactory();
        return new KafkaTemplate<String, AccountCreatedEvent>(factory) {
            @Override
            public CompletableFuture<SendResult<String, AccountCreatedEvent>> send(String topic, AccountCreatedEvent data) {
                log.warn("Kafka is disabled. AccountCreatedEvent to topic '{}' will NOT be sent. AccountId: {}, Email: {}", 
                    topic, data != null ? data.getId() : "null", data != null ? data.getEmail() : "null");
                CompletableFuture<SendResult<String, AccountCreatedEvent>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }
}
