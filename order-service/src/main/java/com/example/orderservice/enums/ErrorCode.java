package com.example.orderservice.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    //11xx
    INVALID_KEY(1100, "Invalid uncategorized error", HttpStatus.BAD_REQUEST),
    INVALID_JSON(1101, "Json invalid", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(1118, "Invalid Status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1119, "Invalid Request", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1120, "Order not found", HttpStatus.NOT_FOUND),
    INVALID_QUANTITY(1121, "Invalid quantity", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD(1122, "Invalid PaymentMethod", HttpStatus.BAD_REQUEST),
    //12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1203, "Product not found", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND(1204, "Cart not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(1205, "Address not found", HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(1206, "Store not found", HttpStatus.NOT_FOUND),
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatusCode statusCode;
}


