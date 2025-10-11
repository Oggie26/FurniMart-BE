package com.example.notificationservice.response;

import com.example.notificationservice.enums.EnumRole;
import com.example.notificationservice.enums.EnumStatus;
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
    private String id;
    private String fullName;
    private String email;
    private String phone;
    private Boolean gender;
    private Date birthday;
    private String avatar;
    private String cccd;
    private Integer point;
    private EnumRole role;
    private EnumStatus status;
    private Date createdAt;
    private Date updatedAt;
}
