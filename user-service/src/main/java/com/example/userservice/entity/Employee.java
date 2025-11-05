package com.example.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employees")
public class Employee extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true, nullable = false)
    String code;

    @Column
    String fullName;

    @Column(unique = true, length = 20)
    String cccd;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}

