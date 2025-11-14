package com.example.deliveryservice.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Boolean gender;
    private Date birthday;
    private String avatar;
    // Removed: cccd, point, role, status, createdAt, updatedAt
    // These fields are not needed for delivery staff
}

