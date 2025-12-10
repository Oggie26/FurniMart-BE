package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationItemResponse {

    private String id;
    private Integer rowLabel;
    private Integer columnNumber;
    private String code;
    private String description;
    private Integer quantity;
    private Integer currentQuantity;
    private EnumStatus status;
    private List<InventoryItemResponse> itemResponse;
}
