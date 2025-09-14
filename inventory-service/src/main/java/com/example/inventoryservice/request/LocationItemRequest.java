package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumRowLabel;
import com.example.inventoryservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LocationItemRequest {

    @NotNull(message = "Zone ID không được để trống")
    private String zoneId;

    @NotNull(message = "Hàng (rowLabel) không được để trống")
    private EnumRowLabel rowLabel;

    @NotNull(message = "Số cột không được để trống")
    @Min(value = 1, message = "Số cột phải lớn hơn hoặc bằng 1")
    private Integer columnNumber;

    @NotNull(message = "Số Code không được để trống")
    private String code;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    private String description;
}
