package com.example.userservice.config;

import com.example.userservice.event.UserCreatedEvent;
import com.example.userservice.event.UserPlacedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private final String BOOTSTRAP_SERVERS = "localhost:9092";

    private Map<String, Object> baseConfigs(String groupId) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        return props;
    }

    @Bean
    public ConsumerFactory<String, UserPlacedEvent> userPlacedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConfigs("user-placed-group"),
                (Deserializer) new StringDeserializer(),
                new JsonDeserializer<>(UserPlacedEvent.class, false));
    }

    @Bean(name = "userKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, UserPlacedEvent> userPlacedKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, UserPlacedEvent>();
        factory.setConsumerFactory(userPlacedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, UserCreatedEvent> userCreatedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseConfigs("user-created-group"),
                (Deserializer) new StringDeserializer(),
                new JsonDeserializer<>(UserCreatedEvent.class, false));
    }

    @Bean(name = "userCreatedKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent> userCreatedKafkaListenerContainerFactory() {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, UserCreatedEvent>();
        factory.setConsumerFactory(userCreatedConsumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, UserPlacedEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, UserPlacedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
