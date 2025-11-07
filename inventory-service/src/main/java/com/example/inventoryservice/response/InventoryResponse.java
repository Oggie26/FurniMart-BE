package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Warehouse;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.EnumTypes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}