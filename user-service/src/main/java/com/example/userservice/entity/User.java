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
public class User extends AbstractEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(unique = true)
    String username;

    @Column
    String password;

    @Column(unique = true)
    String email;

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

    @Enumerated(EnumType.STRING)
    EnumRole role;

    @Column(unique = true, length = 20)
    String cccd;


}
