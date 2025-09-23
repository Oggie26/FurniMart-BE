package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryResponse {

    private String id;
    private String productId;
    private Integer quantity;
    private Integer max_quantity;
    private Integer min_quantity;
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
    private LocationItem locationItem;


}
