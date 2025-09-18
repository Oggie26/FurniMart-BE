package com.example.userservice.entity;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

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

    @Column
    Integer point;

    @Column(unique = true, length = 20)
    String cccd;

    @Column
    String department;

    @Column
    String position;

    @Column
    Double salary;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER)
    List<Address> addresses;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    List<Blog> blogs;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserStore> userStores;
}
