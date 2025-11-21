package com.example.deliveryservice.enums;

public enum EnumProcessOrder {
    PRE_ORDER,
    PENDING,
    PAYMENT,
    ASSIGN_ORDER_STORE,
    MANAGER_ACCEPT,
    READY_FOR_INVOICE,  // Sẵn sàng để tạo PDF/hóa đơn (sau MANAGER_ACCEPT)
    MANAGER_REJECT,
    CONFIRMED,
    PACKAGED,
    SHIPPING,
    DELIVERED,
    FINISHED,
    CANCELLED
}
