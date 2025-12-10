package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory_reserved_warehouse")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedWarehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long orderId;

    @Column
    private String warehouseId;

    @Column
    private String warehouseName;

    @Column
    private int reservedQuantity;

    private Boolean isAssignedWarehouse = false;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Inventory inventory;
}
