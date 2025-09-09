package com.example.userservice.service;


import com.example.userservice.entity.User;
import com.example.userservice.event.UserPlacedEvent;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, UserPlacedEvent> kafkaTemplate;
    private final ApplicationEventPublisher publisher;
    private final PasswordEncoder passwordEncoder;



    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .birthday(user.getBirthday())
                .gender(user.getGender())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .status(user.getStatus())
                .build();
    }
}

