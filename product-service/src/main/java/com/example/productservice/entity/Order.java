package com.example.productservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order extends AbstractEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "total_amount")
    Double totalAmount;

    @Column(name = "username")
    String username;

    @Column(name = "order_date")
    LocalDateTime orderDate;

    @Column(name = "updated_at")
    Date updatedAt;

    @Column(name = "updated_by")
    String updatedBy;

    @Column
    String addressId;

    @Column
    String userId;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<ProcessOrder> processOrders;


}


