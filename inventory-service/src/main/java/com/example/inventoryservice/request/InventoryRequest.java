package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {

    private Long id;

    @NotBlank(message = "Employee ID không được để trống")
    private String employeeId;

    @NotNull(message = "Type không được để trống")
    private EnumTypes type;

    @NotNull(message = "Purpose không được để trống")
    private EnumPurpose purpose;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    private String warehouseId;
}
