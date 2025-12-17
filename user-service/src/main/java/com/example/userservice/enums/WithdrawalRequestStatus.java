package com.example.userservice.enums;

public enum WithdrawalRequestStatus {
    PENDING_APPROVAL,  // Chờ admin duyệt
    APPROVED,          // Admin đã duyệt, đang xử lý
    REJECTED,         // Admin từ chối
    PROCESSING,        // Đang xử lý rút tiền (đã gọi VNPay)
    COMPLETED,         // Hoàn thành
    FAILED,            // Thất bại
    CANCELLED          // User hủy hoặc admin hủy
}
