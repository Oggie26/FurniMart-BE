package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryRequest {

    @NotBlank(message = "Product ID is required")
    private String productId;

    @Min(value = 0, message = "Quantity must be at least 0")
    private int quantity;

    @Min(value = 0, message = "Minimum quantity must be at least 0")
    private int minQuantity;

    @Min(value = 0, message = "Maximum quantity must be at least 0")
    private int maxQuantity;

    @NotBlank(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    @NotBlank(message = "LocationItem ID is required")
    private String locationItemId;

}
