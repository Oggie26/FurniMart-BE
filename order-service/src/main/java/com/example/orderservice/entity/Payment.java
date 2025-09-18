package com.example.orderservice.entity;

import com.example.orderservice.enums.PaymentMethod;
import com.example.orderservice.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column
    private Date date;

    @Column
    private Double total;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(unique = true, updatable = false)
    private String transactionCode;

    @Column
    private String userId;

    @OneToOne
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;


    @PrePersist
    public void prePersist() {
        if (this.transactionCode == null) {
            this.transactionCode = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
    }
}
