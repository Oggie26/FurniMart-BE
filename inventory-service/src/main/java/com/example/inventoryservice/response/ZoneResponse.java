package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumZone;
import com.example.inventoryservice.enums.ZoneStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ZoneResponse {
    private String id;
    private String zoneName;
    private String description;
    @Enumerated(EnumType.STRING)
    private ZoneStatus status;
    @Enumerated(EnumType.STRING)
    private EnumZone zoneCode;
    private Integer quantity;

}
