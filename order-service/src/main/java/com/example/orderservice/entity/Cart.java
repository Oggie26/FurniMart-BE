package com.example.orderservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    Set<CartItem> items = new HashSet<>();

    @Column
    private Double totalPrice = 0.0;

    public void updateTotalPrice() {
        double totalPrice = 0;
        for (CartItem cartItem : items) {
            if (cartItem != null && cartItem.getProductColorId() != null) {
                totalPrice += cartItem.getPrice() * cartItem.getQuantity();
            }
        }
        this.totalPrice = totalPrice;
    }
}