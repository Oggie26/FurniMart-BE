package com.example.orderservice.entity;

import com.example.orderservice.enums.EnumProcessOrder;
import com.example.orderservice.enums.OrderType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "orders")
@ToString
@Builder
public class Order extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column()
    private String userId;

    @Column
    private String storeId;

    @Column()
    private Long addressId;

    @Column(nullable = false)
    private Double total;

    @Enumerated(EnumType.STRING)
    private EnumProcessOrder status;

    @Column
    private String reason;

    @Column
    private String note;

    @Column(name = "deposit_price")
    private Double depositPrice;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcessOrder> processOrders;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    private List<Voucher> vouchers;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Warranty> warranties;

    @Column(name = "qr_code", unique = true)
    private String qrCode;

    @Column(name = "qr_code_generated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date qrCodeGeneratedAt;

    @Column(name = "pdf_file_path")
    private String pdfFilePath;

    @Column
    private Date deadline;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = new Date();
        }

        if (deadline == null) {
            long sevenDays = 7L * 24 * 60 * 60 * 1000;
            deadline = new Date(orderDate.getTime() + sevenDays);
        }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    @Builder.Default
    private OrderType orderType = OrderType.NORMAL;

    @Column(name = "warranty_claim_id")
    private Long warrantyClaimId; // Reference to original warranty claim

    @Column(name = "rejection_count")
    @Builder.Default
    private Integer rejectionCount = 0; // Track số lần bị reject

    @Column(name = "last_rejected_store_id")
    private String lastRejectedStoreId; // Store cuối cùng reject

    // @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
    // private DeliveryConfirmation deliveryConfirmation;
}
