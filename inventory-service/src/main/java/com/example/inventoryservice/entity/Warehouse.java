package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.WarehouseStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "warehouse")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "warehouse_name", nullable = false)
    private String warehouseName;

    @Column
    private Integer capacity;

    @Column
    private String storeId;

    @Enumerated(EnumType.STRING)
    private WarehouseStatus status;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Zone> zones;

    @OneToMany(mappedBy = "warehouse", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Inventory> inventories;

}
