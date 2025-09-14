package com.example.userservice.request;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class StoreRequest {
    
    @NotBlank(message = "Tên cửa hàng không được để trống")
    private String name;
    
    private String city;
    
    private String war;
    
    private String street;
    
    private String addressLine;
    
    private String latitude;
    
    private String longitude;
    
    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
}
