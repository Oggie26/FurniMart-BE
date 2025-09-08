package com.example.notificationservice.config;

import com.example.notificationservice.event.OrderPlacedEvent;
import com.example.notificationservice.event.UserPlacedEvent;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private final String BOOTSTRAP_SERVERS = "localhost:9092";
    private final String GROUP_ID = "notification-group";

    private Map<String, Object> baseConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }

    @Bean
    public ConsumerFactory<String, OrderPlacedEvent> orderConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConfigs(),
                (Deserializer) new StringDeserializer(),
                new JsonDeserializer<>(OrderPlacedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent> orderKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, OrderPlacedEvent>();
        factory.setConsumerFactory(orderConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UserPlacedEvent> userConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConfigs(),
                (Deserializer) new StringDeserializer(),
                new JsonDeserializer<>(UserPlacedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, UserPlacedEvent> userKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, UserPlacedEvent>();

        factory.setConsumerFactory(userConsumerFactory());
        return factory;
    }
}
