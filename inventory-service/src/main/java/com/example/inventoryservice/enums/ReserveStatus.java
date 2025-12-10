package com.example.inventoryservice.enums;


public enum ReserveStatus {
    FULL_FULFILLMENT,   // Đủ hàng 100% -> Cho phép thanh toán ngay
    PARTIAL_FULFILLMENT,// Thiếu hàng -> Hiện popup cảnh báo khách
    OUT_OF_STOCK        // Hết sạch -> Disable nút thanh toán
}
