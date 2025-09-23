package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.EnumTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@AllArgsConstructor
@Entity
@Table(name = "inventory_transaction")
@Setter
@Getter
@NoArgsConstructor
@Builder
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transactionId")
    private Long id;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "dateLocal", nullable = false)
    private LocalDateTime dateLocal;

    @Enumerated(EnumType.STRING)
    private EnumTypes type;

    @Column(name = "note")
    private String note;

    @Column
    private String productId;

    @Column
    private String userId;

    @ManyToOne
    @JoinColumn(name = "warehouseId",  nullable = false)
    private Warehouse warehouse;

}