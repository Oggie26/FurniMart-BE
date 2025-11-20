package com.example.orderservice.entity;

import com.example.orderservice.enums.EnumProcessOrder;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @Column(nullable = false)
    private String userId;

    @Column
    private String storeId;

    @Column(nullable = false)
    private Long addressId;

    @Column(nullable = false)
    private Double total;

    @Enumerated(EnumType.STRING)
    private EnumProcessOrder status;

    @Column
    private String reason;

    @Column
    private String note;

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

//    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL)
//    private DeliveryConfirmation deliveryConfirmation;
}
