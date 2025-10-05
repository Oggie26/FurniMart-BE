package com.example.inventoryservice.request;

import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.EnumTypes;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransactionRequest {

    @NotNull(message = "Transaction type is required")
    private EnumTypes transactionType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be >= 0")
    private BigDecimal unitPrice;

    private String note;

    @NotNull(message = "Inventory ID is required")
    private String inventoryId;

    @NotNull
    private String LocationItemId;

    @NotNull(message = "Product ID is required")
    private String productColorId;

    @NotNull(message = "Status is required")
    private EnumStatus status;

    private String sourceWarehouseId;
    private String stockOrderItemId;
    private String destinationWarehouseId;
}
