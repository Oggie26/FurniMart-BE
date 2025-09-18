package com.example.userservice.response;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StaffResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Boolean gender;
    private Date birthday;
    private String avatar;
    private String cccd;
    private String department;
    private String position;
    private Double salary;
    @Enumerated(EnumType.STRING)
    private EnumRole role;
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
    private Date createdAt;
    private Date updatedAt;
}
