package com.example.deliveryservice.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum  ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    // 11xx
    INVALID_KEY(1100, "Invalid uncategorized error", HttpStatus.BAD_REQUEST),
    INVALID_JSON(1101, "Json invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1102, "Otp expired", HttpStatus.BAD_REQUEST),
    INVALID_OTP(1103, "Invalid Otp", HttpStatus.BAD_REQUEST),
    INVALID_LOGIN(1105, "Invalid Login", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1106, "Invalid Password", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(1118, "Invalid Status", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST(1119, "Invalid Request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_CHANGE_ROLE(1120, "No right to change ", HttpStatus.UNAUTHORIZED),
    INTERNAL_SERVER_ERROR(1121, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    // 12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
    USER_BLOCKED(1203, "User blocked", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1204, "User has existed", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_FOUND(1205, "Email not found", HttpStatus.NOT_FOUND),
    EMAIL_EXISTED(1206, "Email has existed", HttpStatus.BAD_REQUEST),
    USER_DELETED(1207, "User has deleted", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(1208, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_NAME_NOT_FOUND(1209, "Category name not found", HttpStatus.NOT_FOUND),
    CATEGORY_EXISTED(1210, "Category has existed", HttpStatus.BAD_REQUEST),
    PRODUCT_NAME_EXISTED(1216, "Product name has existed", HttpStatus.BAD_REQUEST),
    PRODUCT_EXISTED(1217, "Product has existed", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(1218, "Product not found", HttpStatus.NOT_FOUND),
    MATERIAL_NOT_FOUND(1219, "Material not found", HttpStatus.NOT_FOUND),
    MATERIAL_NAME_NOT_FOUND(1220, "Material name not found", HttpStatus.NOT_FOUND),
    MATERIAL_EXISTED(1221, "Material has existed", HttpStatus.BAD_REQUEST),
    MATERIAL_NAME_EXISTED(1222, "Material name has existed", HttpStatus.BAD_REQUEST),
    CODE_EXISTED(1223, "Code has existed", HttpStatus.BAD_REQUEST),
    CODE_NOT_FOUND(1224, "Code not found", HttpStatus.NOT_FOUND),
    COLOR_EXISTED(1225, "Color has existed", HttpStatus.BAD_REQUEST),
    COLOR_NOT_FOUND(1226, "Color not found", HttpStatus.NOT_FOUND),
    COLOR_NAME_EXISTED(1227, "Color name has existed", HttpStatus.BAD_REQUEST),
    COLOR_NAME_NOT_FOUND(1228, "Color name not found", HttpStatus.NOT_FOUND),
    HEX_CODE_EXISTED(1229, "Hex code has existed", HttpStatus.BAD_REQUEST),
    HEX_CODE_NOT_FOUND(1230, "Hex code not found", HttpStatus.NOT_FOUND),
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
