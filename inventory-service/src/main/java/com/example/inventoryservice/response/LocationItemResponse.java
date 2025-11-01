package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumRowLabel;
import com.example.inventoryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationItemResponse {

    private String id;
    private EnumRowLabel rowLabel;
    private Integer columnNumber;
    private String code;
    private String description;
    private Integer quantity;
    private EnumStatus status;
}
