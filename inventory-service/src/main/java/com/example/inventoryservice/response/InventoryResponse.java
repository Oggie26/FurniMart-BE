package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private String employeeId;
    private EnumTypes type;
    private EnumPurpose purpose;
    private LocalDate date;
    private String note;
    private String warehouseName;
    private String warehouseId;
    private List<InventoryItemResponse> itemResponseList;
}