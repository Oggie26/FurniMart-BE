//package com.example.inventoryservice.entity;
//
//import com.example.inventoryservice.enums.EnumStatus;
//import com.example.inventoryservice.enums.EnumTypes;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import jakarta.persistence.*;
//import jakarta.validation.constraints.Min;
//import jakarta.validation.constraints.NotNull;
//import lombok.*;
//
//import java.math.BigDecimal;
//
//@Entity
//@Table(name = "inventory_transaction")
//@Getter
//@Setter
//@Builder
//@NoArgsConstructor
//@AllArgsConstructor
//public class InventoryTransaction extends AbstractEntity {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.UUID)
//    @Column(nullable = false, updatable = false)
//    private String id;
//
//    @Column(length = 500)
//    private String note;
//
//    @NotNull
//    @Min(1)
//    @Column(nullable = false)
//    private Integer quantity;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal unitPrice;
//
//    @Column(precision = 15, scale = 2)
//    private BigDecimal totalPrice;
//
//    @NotNull
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private EnumTypes type;
//
//    @NotNull
//    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
//    private EnumStatus status;
//
//    @ManyToOne
//    @JoinColumn(name = "inventory_id", nullable = false)
//    private Inventory inventory;
//
//    @PrePersist
//    @PreUpdate
//    public void recalculateTotalPrice() {
//        if (unitPrice != null && quantity != null && quantity > 0) {
//            this.totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
//        } else {
//            this.totalPrice = BigDecimal.ZERO;
//        }
//    }
//}
