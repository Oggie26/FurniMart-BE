package com.example.userservice.request;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Past;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {

    private String fullName;
    
    private String phone;
    
    private String avatar;
    
    private Boolean gender;
    
    @Past(message = "Birthday must be in the past")
    private Date birthday;
    
    private EnumStatus status;
    
    private String cccd;
    
    private Integer point;
    
    // New fields for employee management
    private String storeId;
    
    @Enumerated(EnumType.STRING)
    private EnumRole role;
}
