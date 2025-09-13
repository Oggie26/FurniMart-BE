package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@EnableFeignClients(basePackages = "com.example.orderservice.feign")
public class OrderController {
    private final OrderService orderService;
//    @GetMapping
//    public String getAllOrders() {
//        return "📦 Danh sách đơn hàng (fake)";
//    }
//
//    @PostMapping
//    public String createOrder() {
//        return "✅ Đã tạo đơn hàng (fake)";
//    }

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        return orderService.createOrder(order);
    }
}
