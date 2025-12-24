package com.example.userservice.config;

import com.example.userservice.event.AccountCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.concurrent.CompletableFuture;

@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
public class KafkaOptionalConfig {


    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
    @SuppressWarnings("null")
    public KafkaTemplate<String, Object> kafkaTemplate() {

        return new KafkaTemplate<String, Object>(null) {
            @Override
            @NonNull
            public CompletableFuture<SendResult<String, Object>> send(@NonNull String topic, @Nullable Object data) {
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
            
            @Override
            @NonNull
            public CompletableFuture<SendResult<String, Object>> send(@NonNull String topic, @NonNull String key, @Nullable Object data) {
                CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
    @Nullable
    public ProducerFactory<String, Object> producerFactory() {
        return null;
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
    @SuppressWarnings("null")
    public KafkaTemplate<String, AccountCreatedEvent> accountCreatedEventKafkaTemplate() {

        return new KafkaTemplate<String, AccountCreatedEvent>(null) {
            @Override
            @NonNull
            public CompletableFuture<SendResult<String, AccountCreatedEvent>> send(@NonNull String topic, @Nullable AccountCreatedEvent data) {

                CompletableFuture<SendResult<String, AccountCreatedEvent>> future = new CompletableFuture<>();
                future.complete(null);
                return future;
            }
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false", matchIfMissing = false)
    @Nullable
    public ProducerFactory<String, AccountCreatedEvent> accountCreatedEventProducerFactory() {
        return null;
    }
}
