package com.example.productservice.entity;

import com.example.productservice.enums.EnumProcessOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class ProcessOrder extends  AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    EnumProcessOrder process;

    @Column
    LocalDate time;

    @ManyToOne
    @JoinColumn(name = "order_id")
    @JsonIgnore
    Order order;

}
