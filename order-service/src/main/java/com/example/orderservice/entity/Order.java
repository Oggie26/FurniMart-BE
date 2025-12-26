package com.example.orderservice.entity;

import com.example.orderservice.enums.ComplaintStatus;
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

    @Builder.Default
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDate = new Date();

    @Builder.Default
    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline = new Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000);

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

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    @Builder.Default
    private OrderType orderType = OrderType.NORMAL;

    @Column(name = "warranty_claim_id")
    private Long warrantyClaimId;

    @Column(name = "rejection_count")
    @Builder.Default
    private Integer rejectionCount = 0;

    @Column(name = "last_rejected_store_id")
    private String lastRejectedStoreId;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "complaint_reason", columnDefinition = "TEXT")
    private String complaintReason;

    @Column(name = "complaint_date")
    @Temporal(TemporalType.TIMESTAMP)
    private Date complaintDate;

    @Column(name = "is_store_error")
    private Boolean isStoreError;

    @Column(name = "customer_refused")
    private Boolean customerRefused;

    @Column(name = "customer_contactable")
    private Boolean customerContactable;

    @Column(name = "complaint_evidence_photos", columnDefinition = "TEXT")
    private String complaintEvidencePhotos; // JSON array of photo URLs

    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_status")
    private ComplaintStatus complaintStatus;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
}
