package com.example.userservice.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    //11xx
    INVALID_KEY(1100, "Invalid uncategorized error", HttpStatus.BAD_REQUEST),
    INVALID_JSON(1101, "Json invalid", HttpStatus.BAD_REQUEST),
    INVALID_LOGIN(1105, "Invalid Login", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1106, "Invalid Password", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(1118, "Invalid Status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1119, "Invalid Request", HttpStatus.BAD_REQUEST),
    EXTERNAL_SERVICE_ERROR(1120, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_DELETED(1121, "User Deleted", HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(1122, "Invalid Token", HttpStatus.BAD_REQUEST),
    INTERNAL_SERVER_ERROR(1123, "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    //12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
    USER_BLOCKED(1203, "User blocked", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_FOUND(1204, "Emal not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTS(1205, "Email already exists", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND(1206, "Account not found for user", HttpStatus.NOT_FOUND),
    PHONE_NOT_FOUND(1207, "Phone not found", HttpStatus.BAD_REQUEST),
    PHONE_EXISTS(1208, "Phone already exists", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1209, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1210, "User not found", HttpStatus.NOT_FOUND),
    BLOG_NOT_FOUND(1211, "Blog not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(1212, "Address not found", HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(1213, "Store not found", HttpStatus.NOT_FOUND),
    USER_STORE_RELATIONSHIP_EXISTS(1214, "User store relationship already exists", HttpStatus.BAD_REQUEST),
    USER_STORE_RELATIONSHIP_NOT_FOUND(1215, "User store relationship not found", HttpStatus.NOT_FOUND),
    
    // Chat related errors
    CHAT_NOT_FOUND(1216, "Chat not found", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(1217, "Message not found", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(1218, "Access denied", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(1219, "Unauthorized", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_PARTICIPANT(1220, "User is already a participant", HttpStatus.BAD_REQUEST),
    USER_NOT_PARTICIPANT(1221, "User is not a participant", HttpStatus.BAD_REQUEST),

    // Wallet related errors
    WALLET_NOT_FOUND(1222, "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_CODE_EXISTS(1223, "Wallet code already exists", HttpStatus.BAD_REQUEST),
    USER_ALREADY_HAS_WALLET(1224, "User already has a wallet", HttpStatus.BAD_REQUEST),
    WALLET_NOT_ACTIVE(1225, "Wallet is not active", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(1226, "Insufficient wallet balance", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND(1227, "Transaction not found", HttpStatus.NOT_FOUND),
    TRANSACTION_CODE_EXISTS(1228, "Transaction code already exists", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(1229, "Order not found", HttpStatus.NOT_FOUND),
    
    // Employee management errors
    INVALID_ROLE(1230, "Invalid role for employee operations", HttpStatus.BAD_REQUEST),
    CANNOT_UPDATE_CUSTOMER_ROLE(1231, "Cannot update customer role", HttpStatus.FORBIDDEN),
    CANNOT_CREATE_ADMIN_ROLE(1232, "Cannot create ADMIN role through employee API", HttpStatus.FORBIDDEN),
    CANNOT_CREATE_CUSTOMER_THROUGH_EMPLOYEE_API(1233, "Cannot create CUSTOMER role through employee API", HttpStatus.FORBIDDEN),
    
    // Address related errors
    DUPLICATE_ADDRESS(1234, "Address already exists", HttpStatus.BAD_REQUEST),
    
    // Favorite product related errors
    PRODUCT_ALREADY_FAVORITE(1235, "Product is already in favorites", HttpStatus.BAD_REQUEST),
    FAVORITE_PRODUCT_NOT_FOUND(1236, "Favorite product not found", HttpStatus.NOT_FOUND),
    
    // Chat mode related errors
    CHAT_NOT_READY(1237, "Chat is not ready for messaging", HttpStatus.BAD_REQUEST),
    CHAT_ALREADY_ACCEPTED(1238, "Chat request has already been accepted", HttpStatus.BAD_REQUEST),
    INVALID_CHAT_STATE(1239, "Không thể gửi tin nhắn khi đang chờ nhân viên. Vui lòng đợi nhân viên kết nối.", HttpStatus.BAD_REQUEST),
    STAFF_NOT_ONLINE(1240, "No staff is currently online", HttpStatus.BAD_REQUEST),
    AI_SERVICE_UNAVAILABLE(1241, "AI service is currently unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    AI_BOT_NOT_CONFIGURED(1242, "AI bot user is not configured. Please contact administrator", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(1243, "Service temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    
    // Email verification and password reset errors
    EMAIL_NOT_VERIFIED(1253, "Email is not verified", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_EXPIRED(1254, "Verification token has expired", HttpStatus.BAD_REQUEST),
    VERIFICATION_TOKEN_INVALID(1255, "Invalid verification token", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_EXPIRED(1256, "Reset token has expired", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_INVALID(1257, "Invalid reset token", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1258, "OTP code has expired", HttpStatus.BAD_REQUEST),
    OTP_INVALID(1259, "Invalid OTP code", HttpStatus.BAD_REQUEST),
    OTP_REQUIRED(1260, "OTP verification required", HttpStatus.BAD_REQUEST),
    OTP_LOCKED(1261, "OTP verification locked due to too many failed attempts. Please try again later", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED(1262, "Too many requests. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    
    // Withdrawal request related errors
    WITHDRAWAL_REQUEST_NOT_FOUND(1263, "Withdrawal request not found", HttpStatus.NOT_FOUND),
    WITHDRAWAL_REQUEST_CODE_EXISTS(1264, "Withdrawal request code already exists", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_REQUEST_INVALID_STATUS(1265, "Invalid status for this operation", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_REQUEST_ALREADY_PROCESSED(1266, "Withdrawal request has already been processed", HttpStatus.BAD_REQUEST),
    WITHDRAWAL_REQUEST_CANNOT_CANCEL(1267, "Cannot cancel withdrawal request in current status", HttpStatus.BAD_REQUEST),
    
    // Blog related errors
    BLOG_ACCESS_DENIED(1268, "Access denied to blog", HttpStatus.FORBIDDEN),
    BLOG_HIERARCHY_DENIED(1269, "Cannot manage blog of peer or superior", HttpStatus.FORBIDDEN),
    BLOG_TOGGLE_STATUS_DENIED(1270, "Cannot toggle blog status", HttpStatus.FORBIDDEN),

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


