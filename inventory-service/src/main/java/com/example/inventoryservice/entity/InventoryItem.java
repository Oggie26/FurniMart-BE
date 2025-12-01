package com.example.inventoryservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"inventory", "locationItem"})
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String productColorId;

    @Column
    private Integer reservedQuantity = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_item_id")
    private LocationItem locationItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_id", nullable = false)
    private Inventory inventory;
}
