package com.example.orderservice.event;

import com.example.orderservice.entity.Payment;
import com.example.orderservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String userId;
    private String addressLine;
    private List<OrderItem> items;
    private PaymentMethod paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productColorId;
        private int quantity;
    }
}
