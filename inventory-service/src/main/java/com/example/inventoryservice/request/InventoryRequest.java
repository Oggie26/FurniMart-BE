package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {

    private Long id;

    @NotNull(message = "Type không được để trống")
    private EnumTypes type;

    @NotNull(message = "Purpose không được để trống")
    private EnumPurpose purpose;

    @Size(max = 255, message = "Ghi chú không được vượt quá 255 ký tự")
    private String note;

    @NotNull(message = "WarehouseId không được để trống")
    private String warehouseId;

    private String toWarehouseId;

    private Long orderId;

    @NotEmpty(message = "Phải có ít nhất 1 sản phẩm")
    private List<InventoryItemRequest> items;
}
