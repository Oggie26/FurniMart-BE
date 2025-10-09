package com.example.notificationservice.enums;

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
    WAREHOUSE_NOT_FOUND(1121, "Warehouse not found", HttpStatus.NOT_FOUND),
    NOT_FOUND_USER( 1122,"Not found user", HttpStatus.NOT_FOUND),
    NOT_FOUND_ORDER(1123,"Not found order", HttpStatus.NOT_FOUND),
    LOCATIONITEM_EXISTS(1150, "Location item already exists", HttpStatus.CONFLICT),
    COLUMNNUMBER_NOT_FOUND(1151, "Column number not found", HttpStatus.NOT_FOUND),
    COLUMNNUMBER_EXISTS(1152, "Column number already exists", HttpStatus.CONFLICT),
    ROWLABEL_NOT_FOUND(1153, "Rowlabel not found", HttpStatus.NOT_FOUND),
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


