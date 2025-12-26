package com.example.orderservice.enums;

public enum ComplaintStatus {
    PENDING_REVIEW,  // Chờ admin/staff review
    APPROVED,        // Đã được duyệt (có thể hoàn tiền nếu đủ điều kiện)
    REJECTED         // Bị từ chối (không hoàn tiền)
}

