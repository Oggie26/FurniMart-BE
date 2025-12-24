package com.example.inventoryservice.enums;

public enum TransferStatus {
    PENDING,   // Chưa duyệt
    PENDING_CONFIRM,
    ACCEPTED,  // Đồng ý chuyển
    CANCELLED,
    FINISHED,
    REJECTED   // Từ chối
}
