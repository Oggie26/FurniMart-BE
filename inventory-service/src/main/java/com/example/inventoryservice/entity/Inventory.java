package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventory extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumTypes type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumPurpose purpose;

    @Column(nullable = false)
    private LocalDate date;

    @Column(columnDefinition = "text")
    private String note;

    @Column(columnDefinition = "text")
    private String pdfUrl;

    @Column
    private Long orderId;

    @Column(unique = true, nullable = false, columnDefinition = "text")
    private String code;

    @Column()
    private String toWarehouseId;

    @Column()
    private String toWarehouseName;

    @Column(name = "transfer_status")
    @Enumerated(EnumType.STRING)
    private TransferStatus transferStatus;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryItem> inventoryItems = new ArrayList<>();

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryReservedWarehouse> reservedWarehouses = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (this.code == null || this.code.isEmpty()) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            this.code = "INV-" + datePart + "-" + randomPart;
        }
    }
}
