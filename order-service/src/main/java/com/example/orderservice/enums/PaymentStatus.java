package com.example.orderservice.enums;

import org.springframework.http.HttpStatus;

public enum PaymentStatus {
    NOT_PAID,
    PAID,
    DEPOSITED,
    PENDING,
    FAILED
}
