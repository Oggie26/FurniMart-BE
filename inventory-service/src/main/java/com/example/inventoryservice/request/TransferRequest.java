package com.example.inventoryservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {

    @NotBlank
    private String fromWarehouseId;

    @NotBlank
    private String toWarehouseId;

    @NotEmpty
    private List<InventoryItemRequest> items;

    private String note;
}
