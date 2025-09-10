package com.example.userservice.entity;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "users")
public class User extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;
}
