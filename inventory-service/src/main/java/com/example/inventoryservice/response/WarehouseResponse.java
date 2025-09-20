package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.WarehouseStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private String id;
    private String warehouseName;
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;
    private Integer capacity;
    private List<Zone> zone;

}
