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
    SUPPLIER_NOT_FOUND(1127, "Supplier not found", HttpStatus.NOT_FOUND),
    SUPPLIER_EXISTS(1128, "Supplier already exists", HttpStatus.CONFLICT),
    SUPPLIER_NAME_EXISTS(1129, "Supplier name already exists", HttpStatus.NOT_FOUND),
    EMAIL_EXISTS(1130, "Email already exists", HttpStatus.CONFLICT),
    EMAIL_NOT_FOUND(1131, "Email not found", HttpStatus.NOT_FOUND),
    PHONE_NOT_FOUND(1132, "Phone number already exists", HttpStatus.NOT_FOUND),
    PHONE_EXISTS(1133, "Phone number already exists", HttpStatus.CONFLICT),
    CONTACT_NOT_FOUND(1134, "Contact not found", HttpStatus.NOT_FOUND),
    CONTACT_EXISTS(1135, "Contact already exists", HttpStatus.CONFLICT),
    CONTRACT_FILE_NOT_FOUND(1136, "Contract file not found", HttpStatus.NOT_FOUND),
    CONTRACT_FILE_EXISTS(1137, "Contract file already exists", HttpStatus.CONFLICT),
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
    CONTRACT_NOT_FOUND(1156, "Contract not found", HttpStatus.NOT_FOUND),
    CONTRACT_NAME_NOT_FOUND(1157, "Contract name not found", HttpStatus.NOT_FOUND),
    CONTRACT_CODE_NOT_FOUND(1158, "Contract code not found", HttpStatus.NOT_FOUND),
    ZONE_ALREADY_DELETED(1159, "Zone already deleted", HttpStatus.CONFLICT),
    TRANSACTION_NOT_FOUND(1160, "Transaction not found", HttpStatus.NOT_FOUND),
    TRANSACTION_EXISTS(1161, "Transaction not exists", HttpStatus.CONFLICT),
    NOT_ENOUGH_QUANTITY(1162, "Not enough quantity", HttpStatus.CONFLICT),
    INSUFFICIENT_STOCK(1163, "Insufficient stock", HttpStatus.CONFLICT),
    STOCKORDERITEM_NOT_FOUND(1164, "Stock order item not found", HttpStatus.NOT_FOUND),
    CONTRACT_QUANTITY_EXCEEDED(1165, "Contract quantity exceeded", HttpStatus.CONFLICT),
    UNIT_PRICE_NOT_MATCH_CONTRACT(1166, "Unit price not match contract", HttpStatus.CONFLICT),
    //12xx
    UNAUTHENTICATED(1201, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    NOT_FOUND_USER(1202, "User not found", HttpStatus.NOT_FOUND),
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


