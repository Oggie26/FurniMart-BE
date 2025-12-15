package com.example.notificationservice.event;

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
public class OrderDeliveredEvent {
    private Long orderId;
    private String email;
    private String fullName;
    private Date deliveryDate;
    private String deliveryStaffId;
    private Double totalAmount;
    private List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String productColorId;
        private String productName;
        private Double price;
        private int quantity;
        private String colorName;
    }
}
