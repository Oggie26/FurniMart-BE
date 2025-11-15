package com.example.inventoryservice.enums;

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
    WAREHOUSE_EXISTS(1122, "Warehouse already exists", HttpStatus.CONFLICT),
    USER_ALREADY_ASSIGNED_TO_WAREHOUSE(1123, "User already assigned to Warehouse", HttpStatus.CONFLICT),
    USER_BLOCKED(1124, "User blocked", HttpStatus.CONFLICT),
    INVENTORY_NOT_FOUND(1125, "Inventory not found", HttpStatus.NOT_FOUND),
    INVENTORY_EXISTS(1126, "Inventory already exists", HttpStatus.CONFLICT),
    EMAIL_EXISTS(1130, "Email already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND(1131, "Email not found", HttpStatus.NOT_FOUND),
    PHONE_NOT_FOUND(1132, "Phone number already exists", HttpStatus.NOT_FOUND),
    PHONE_EXISTS(1133, "Phone number already exists", HttpStatus.CONFLICT),
    CODE_NOT_FOUND(1138, "Code not found", HttpStatus.NOT_FOUND),
    CODE_EXISTS(1139, "Code already exists", HttpStatus.CONFLICT),
    TAXCODE_NOT_FOUND(1140, "Tax code not found", HttpStatus.NOT_FOUND),
    TAXCODE_EXISTS(1141, "Tax code already exists", HttpStatus.CONFLICT),
    ZONE_NOT_FOUND(1142, "Zone not found", HttpStatus.NOT_FOUND),
    ZONE_EXISTS(1143, "Zone already exists", HttpStatus.CONFLICT),
    ZONE_CODE_NOT_FOUND(1144, "Zone code not found", HttpStatus.NOT_FOUND),
    ZONE_CODE_EXISTS(1145, "Zone code already exists", HttpStatus.CONFLICT),
    ZONE_NAME_NOT_FOUND(1146, "Zone name not found", HttpStatus.NOT_FOUND),
    ZONE_NAME_EXISTS(1147, "Zone name already exists", HttpStatus.CONFLICT),
    WAREHOUSE_FULL(1148, "Warehouse full", HttpStatus.CONFLICT),
    LOCATIONITEM_NOT_FOUND(1149, "Location item not found", HttpStatus.NOT_FOUND),
    LOCATIONITEM_EXISTS(1150, "Location item already exists", HttpStatus.CONFLICT),
    COLUMNNUMBER_NOT_FOUND(1151, "Column number not found", HttpStatus.NOT_FOUND),
    COLUMNNUMBER_EXISTS(1152, "Column number already exists", HttpStatus.CONFLICT),
    ROWLABEL_NOT_FOUND(1153, "Rowlabel not found", HttpStatus.NOT_FOUND),
    ROWLABEL_EXISTS(1154, "Rowlabel not exists", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(1155, "Product not found", HttpStatus.NOT_FOUND),
    ZONE_ALREADY_DELETED(1159, "Zone already deleted", HttpStatus.CONFLICT),
    TRANSACTION_NOT_FOUND(1160, "Transaction not found", HttpStatus.NOT_FOUND),
    TRANSACTION_EXISTS(1161, "Transaction not exists", HttpStatus.CONFLICT),
    NOT_ENOUGH_QUANTITY(1162, "Not enough quantity", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(1163, "Insufficient stock", HttpStatus.CONFLICT),
    INVALID_WAREHOUSE_STOREID(1167, "Invalid warehouse storeID", HttpStatus.BAD_REQUEST),
    INVALID_QUANTITY_RANGE(1168, "Invalid quantity range", HttpStatus.BAD_REQUEST),
    BAD_REQUEST(1169, "Bad Request", HttpStatus.BAD_REQUEST),
    NOT_FOUND(1170, "Not Found", HttpStatus.NOT_FOUND),
    STORE_ALREADY_HAS_WAREHOUSE(1171,"Store already has warehouse", HttpStatus.CONFLICT),
    //12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
    STORE_NOT_FOUND(1203, "Store not found", HttpStatus.NOT_FOUND),
    ZONE_CAPACITY_EXCEEDED(1204, "Zone capacity exceeded", HttpStatus.CONFLICT),
    EXCEEDS_MAX_QUANTITY(1205,"Exceeds maximum quantity", HttpStatus.CONFLICT),
    BELOW_MIN_QUANTITY(1206,"Below Minimum quantity", HttpStatus.CONFLICT),
    WAREHOUSE_CAPACITY_EXCEEDED(1207, "Warehouse capacity exceeded", HttpStatus.CONFLICT),
    INVALID_TYPE(1208,"Invalid token ", HttpStatus.CONFLICT),
    INVALID_INPUT(1209,"Invalid Input", HttpStatus.BAD_REQUEST)
    ,
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


