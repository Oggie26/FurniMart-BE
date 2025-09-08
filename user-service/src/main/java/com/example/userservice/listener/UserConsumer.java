package com.example.userservice.listener;

import com.example.userservice.entity.User;
import com.example.userservice.event.UserCreatedEvent;
import com.example.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserConsumer {

    private final UserRepository userRepository;

    @KafkaListener(
            topics = "user-created-topic",
            groupId = "user-group",
            containerFactory = "userCreatedKafkaListenerContainerFactory"
    )
    public void handleUserCreated(UserCreatedEvent event) {
    }
}
