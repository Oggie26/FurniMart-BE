package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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

    @Column(length = 255)
    private String note;

    @Column
    private String PdfUrl;

    @Column
    private Long orderId;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = true)
    private String toWarehouseId;

    @Column(nullable = true)
    private String toWarehouseName;

    @Column(name = "transfer_status")
    @Enumerated(EnumType.STRING)
    private TransferStatus transferStatus;

    @ManyToOne
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryItem> inventoryItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (this.code == null || this.code.isEmpty()) {
            String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            this.code = "INV-" + datePart + "-" + randomPart;
        }
    }
}
