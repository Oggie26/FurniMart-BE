package com.example.notificationservice.event;

import com.example.notificationservice.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private Long orderId;
    private String email;
    private String fullName;
    private Date orderDate;
    private Double totalPrice;
    private String addressLine;
    private List<OrderItem> items;
    private PaymentMethod paymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        private String productColorId;
        private Double price;
        private int quantity;
    }
}
