package com.example.orderservice.response;

import com.example.orderservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderForShipperResponse {
    private Long orderId;
    private String customerName;
    private String customerPhone;
    private String deliveryAddress;
    private Double total;
    private Date orderDate;
    private EnumProcessOrder status;
    private String deliveryStatus; // From delivery service
    private String assignedShipperId;
    private String assignedShipperName;
    private Date estimatedDeliveryDate;
}

