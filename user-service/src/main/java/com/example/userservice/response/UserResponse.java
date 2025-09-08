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
public class UserResponse {
    String id;
    String fullName;
    String email;
    String phone;
    Boolean gender;
    Date birthday;
    String avatar;
    @Enumerated(EnumType.STRING)
    EnumRole role;
    @Enumerated(EnumType.STRING)
    EnumStatus status;
    Date createdAt;
    Date updatedAt;
}
