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
    
    @NotBlank(message = "Thành phố không được để trống")
    private String city;
    
    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;
    
    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;
    
    @NotBlank(message = "Tên đường không được để trống")
    private String street;
    
    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String addressLine;
    
    private Double latitude;
    
    private Double longitude;
    
    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
}
