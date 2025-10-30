//package com.example.aiservice.listener;
//
//import com.example.aiservice.response.ProductCreatedEvent;
//import com.example.aiservice.service.GenerationBlogService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.annotation.KafkaListener;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//public class ProductEventListener {
//    private final GenerationBlogService generationBlogService;
//    @KafkaListener(
//            topics = "product-created-topic",
//            groupId = "ai-group",
//            containerFactory = "productCreatedKafkaListenerContainerFactory")
//    public void handleUserCreated(ProductCreatedEvent event) {
//        try {
//            generationBlogService.generationBlogByAi(event);
//        } catch (Exception e) {
//            System.err.println("Lỗi " + e.getMessage());
//
//        }
//    }
//}