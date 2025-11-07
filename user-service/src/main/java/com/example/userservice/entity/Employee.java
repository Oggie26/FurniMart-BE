package com.example.userservice.entity;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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

    @Column(unique = true)
    String phone;

    @Column
    @Temporal(TemporalType.DATE)
    Date birthday;

    @Column
    Boolean gender;

    @Enumerated(EnumType.STRING)
    EnumStatus status;

    @Column
    String avatar;

    @Column(unique = true, length = 20)
    String cccd;

    @Column
    String department;

    @Column
    String position;

    @Column
    BigDecimal salary;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmployeeStore> employeeStores;
}

