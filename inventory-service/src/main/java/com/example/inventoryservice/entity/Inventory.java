package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String productColorId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int minQuantity;

    @Column(nullable = false)
    private int maxQuantity;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_item_id", nullable = false)
    private LocationItem locationItem;
}
