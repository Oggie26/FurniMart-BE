package com.example.orderservice.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    // 11xx
    INVALID_KEY(1100, "Invalid uncategorized error", HttpStatus.BAD_REQUEST),
    INVALID_JSON(1101, "Json invalid", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(1118, "Invalid Status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1119, "Invalid Request", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1120, "Order not found", HttpStatus.NOT_FOUND),
    INVALID_QUANTITY(1121, "Invalid quantity", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_METHOD(1122, "Invalid PaymentMethod", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(1123, "Invalid Address", HttpStatus.BAD_REQUEST),
    // 12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1203, "Product not found", HttpStatus.NOT_FOUND),
    CART_NOT_FOUND(1204, "Cart not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(1205, "Address not found", HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(1206, "Store not found", HttpStatus.NOT_FOUND),

    // Voucher related errors
    VOUCHER_NOT_FOUND(1207, "Voucher not found", HttpStatus.NOT_FOUND),
    VOUCHER_CODE_EXISTS(1208, "Voucher code already exists", HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID(1209, "Voucher is invalid or expired", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_APPLICABLE(1210, "Voucher is not applicable for this order", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_EXCEEDED(1211, "Voucher usage limit exceeded", HttpStatus.BAD_REQUEST),
    COLOR_NOT_FOUND(1212, "Color not found", HttpStatus.NOT_FOUND),
    CART_EMPTY(1213, "Cart is empty", HttpStatus.BAD_REQUEST),
    NOT_IMPLEMENTED(1214, "Not implemented", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(1215, "Cart item not found", HttpStatus.NOT_FOUND),
    OUT_OF_STOCK(1216, "Out of stock ", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_TOTAL(1217, "Invalid order total", HttpStatus.BAD_REQUEST),

    // Warranty related errors
    WARRANTY_NOT_FOUND(1218, "Warranty not found", HttpStatus.NOT_FOUND),
    WARRANTY_CANNOT_BE_CLAIMED(1219, "Warranty cannot be claimed", HttpStatus.BAD_REQUEST),
    WARRANTY_CLAIM_NOT_FOUND(1220, "Warranty claim not found", HttpStatus.NOT_FOUND),
    // Delivery confirmation related errors
    DELIVERY_CONFIRMATION_NOT_FOUND(1220, "Delivery confirmation not found", HttpStatus.NOT_FOUND),
    DELIVERY_CONFIRMATION_ALREADY_EXISTS(1221, "Delivery confirmation already exists", HttpStatus.BAD_REQUEST),
    QR_CODE_ALREADY_SCANNED(1222, "QR code already scanned", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_FOUND(1223, "Payment not found", HttpStatus.NOT_FOUND),
    // Invoice/PDF related errors
    INVOICE_ALREADY_GENERATED(1232, "PDF hóa đơn đã được tạo cho order này", HttpStatus.BAD_REQUEST),

    // Payment retry errors
    PAYMENT_ALREADY_COMPLETED(1224, "Payment has already been completed", HttpStatus.BAD_REQUEST),
    PAYMENT_CANNOT_BE_RETRIED(1225, "Payment cannot be retried", HttpStatus.BAD_REQUEST),
    ORDER_NOT_PENDING_PAYMENT(1226, "Order is not in pending payment state", HttpStatus.BAD_REQUEST),

    // Warranty action errors
    INVALID_WARRANTY_ACTION(1227, "Invalid warranty action type", HttpStatus.BAD_REQUEST),
    WARRANTY_CLAIM_ALREADY_RESOLVED(1228, "Warranty claim has already been resolved", HttpStatus.BAD_REQUEST),
    CANNOT_CREATE_WARRANTY_ORDER(1229, "Cannot create order from warranty claim", HttpStatus.BAD_REQUEST),
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
