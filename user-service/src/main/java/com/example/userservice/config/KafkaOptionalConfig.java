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


    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, Object> kafkaTemplate() {

        return new KafkaTemplate<String, Object>(null) {
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, Object data) {
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
            
            @Override
            public CompletableFuture<SendResult<String, Object>> send(String topic, String key, Object data) {
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, Object> producerFactory() {
        return null;
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedEventKafkaTemplate() {

        return new KafkaTemplate<String, AccountCreatedEvent>(null) {
            @Override
            public CompletableFuture<SendResult<String, AccountCreatedEvent>> send(String topic, AccountCreatedEvent data) {

                CompletableFuture<SendResult<String, AccountCreatedEvent>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = true)
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedEventProducerFactory() {
        return null;
    }
}
