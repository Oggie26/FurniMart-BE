package com.example.deliveryservice.enums;

public enum DeliveryStatus {
    ASSIGNED,           // Đã gán cho delivery staff
    PREPARING,          // Đang chuẩn bị sản phẩm
    READY,              // Sẵn sàng giao hàng
    IN_TRANSIT,         // Đang giao hàng
    DELIVERED,          // Đã giao hàng
    CANCELLED           // Đã hủy
}

