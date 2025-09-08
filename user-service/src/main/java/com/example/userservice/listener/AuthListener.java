//package com.example.userservice.listener;
//
//import com.example.userservice.event.AuthPlacedEvent;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Component;
//
//
//@RequiredArgsConstructor
//@Component
//public class AuthListener {
//    @KafkaListener(topics = "auth-topic", groupId = "auth-group")
//    public void consume(AuthPlacedEvent event) {
//        System.out.println("📨 Nhận được event từ Kafka: " + event);
//        try {
//            emailService.sendOrderEmail(event);
//        } catch (Exception e) {
//            System.err.println("Lỗi rồi fen ơi " + e.getMessage());
//        }
//    }
//}