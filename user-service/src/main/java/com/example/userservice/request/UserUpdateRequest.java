package com.example.userservice.request;

import com.example.userservice.enums.EnumStatus;
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
}
