package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumTypes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionResponse {

    private Long transactionId;

    private int quantity;

    private LocalDateTime dateLocal;

    private EnumTypes type;

    private String note;

    private String productColorId;

    private String userId;

    private String warehouseId;

}