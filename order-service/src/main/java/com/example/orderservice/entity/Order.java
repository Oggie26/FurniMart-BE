package com.example.orderservice.entity;

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
public class Order extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String storeId;

    @Column(nullable = false)
    private Integer addressId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double total;

    @Column
    private String note;

    @Column
    private Date orderDate;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcessOrder> processOrders;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails;
}
