package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumZone;
import com.example.inventoryservice.enums.ZoneStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZoneRequest {

    @NotBlank(message = "Tên zone không được để trống")
    private String zoneName;

    private String description;

    @NotNull(message = "Trạng thái zone không được để trống")
    private ZoneStatus status;

    @NotNull(message = "Mã zone không được để trống")
    private EnumZone zoneCode;

    @Min(value = 0, message = "Số lượng >= 0")
    private Integer quantity;

    @NotBlank(message = "Warehouse ID không được để trống")
    private String warehouseId;

}
