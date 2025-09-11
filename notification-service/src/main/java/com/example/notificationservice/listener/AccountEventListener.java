package com.example.notificationservice.listener;

import com.example.notificationservice.event.AccountPlaceEvent;
import com.example.notificationservice.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountEventListener {
    private final EmailService emailService;

    @KafkaListener(
            topics = "account-created-topic",
            groupId = "notification-group",
            containerFactory = "accountKafkaListenerContainerFactory")
    public void handleUserCreated(AccountPlaceEvent event) {
        try {
            emailService.sendMailRegisterSuccess(event);
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail: " + e.getMessage());

        }
    }
}
