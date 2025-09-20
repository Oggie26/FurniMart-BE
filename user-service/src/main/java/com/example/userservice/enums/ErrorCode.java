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
    PHONE_NOT_FOUND(1206, "Phone not found", HttpStatus.BAD_REQUEST),
    PHONE_EXISTS(1207, "Phone already exists", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1208, "User already exists", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1209, "User not found", HttpStatus.NOT_FOUND),
    BLOG_NOT_FOUND(1210, "Blog not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(1211, "Address not found", HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(1212, "Store not found", HttpStatus.NOT_FOUND),
    USER_STORE_RELATIONSHIP_EXISTS(1213, "User store relationship already exists", HttpStatus.BAD_REQUEST),
    USER_STORE_RELATIONSHIP_NOT_FOUND(1214, "User store relationship not found", HttpStatus.NOT_FOUND),
    
    // Chat related errors
    CHAT_NOT_FOUND(1215, "Chat not found", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(1216, "Message not found", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(1217, "Access denied", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(1218, "Unauthorized", HttpStatus.UNAUTHORIZED),
    USER_ALREADY_PARTICIPANT(1219, "User is already a participant", HttpStatus.BAD_REQUEST),
    USER_NOT_PARTICIPANT(1220, "User is not a participant", HttpStatus.BAD_REQUEST),

    // Wallet related errors
    WALLET_NOT_FOUND(1221, "Wallet not found", HttpStatus.NOT_FOUND),
    WALLET_CODE_EXISTS(1222, "Wallet code already exists", HttpStatus.BAD_REQUEST),
    USER_ALREADY_HAS_WALLET(1223, "User already has a wallet", HttpStatus.BAD_REQUEST),
    WALLET_NOT_ACTIVE(1224, "Wallet is not active", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(1225, "Insufficient wallet balance", HttpStatus.BAD_REQUEST),
    TRANSACTION_NOT_FOUND(1226, "Transaction not found", HttpStatus.NOT_FOUND),
    TRANSACTION_CODE_EXISTS(1227, "Transaction code already exists", HttpStatus.BAD_REQUEST),

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


