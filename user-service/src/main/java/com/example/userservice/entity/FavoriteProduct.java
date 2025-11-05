package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favorite_products", 
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "product_id"})
       })
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FavoriteProduct extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
}

