package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Boolean gender;
    private Date birthday;
    private String avatar;
    private String cccd;
    private Integer point;
    private String role;
    private String status;
    private Date createdAt;
    private Date updatedAt;
}

