package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.WarehouseStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WarehouseRequest {

    @NotBlank(message = "Tên kho không được để trống")
    private String warehouseName;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    @Min(value = 0, message = "Sức chứa phải >= 0")
    private Integer capacity;

}
