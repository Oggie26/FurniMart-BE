package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "com.example.orderservice.feign")
public class OrderController {
    private final OrderServiceImpl orderServiceImpl;


}
