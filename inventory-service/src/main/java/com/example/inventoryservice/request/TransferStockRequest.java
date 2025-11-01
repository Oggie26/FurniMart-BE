package com.example.inventoryservice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferStockRequest {

    @NotBlank(message = "Product color ID is required")
    private String productColorId;

    @NotBlank(message = "From warehouse ID is required")
    private String fromWarehouseId;

    @NotBlank(message = "From zone ID is required")
    private String fromZoneId;

    @NotBlank(message = "From locationItem ID is required")
    private String fromLocationItemId;

    @NotBlank(message = "To warehouse ID is required")
    private String toWarehouseId;

    @NotBlank(message = "To zone ID is required")
    private String toZoneId;

    @NotBlank(message = "To locationItem ID is required")
    private String toLocationItemId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;
}
