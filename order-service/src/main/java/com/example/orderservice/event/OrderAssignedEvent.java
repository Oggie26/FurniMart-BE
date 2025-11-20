package com.example.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderAssignedEvent {
    private Long orderId;
    private String storeId;
    private String managerEmail;
    private String managerName;
    private String pdfFilePath;
    private Double totalPrice;
    private String addressLine;
}

