package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.WarehouseStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseResponse {
    private String id;
    private String warehouseName;
    private String address;
    private Double latitude;
    private Double longitude;
    private String userId;
    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;
    private Integer capacity;

}
