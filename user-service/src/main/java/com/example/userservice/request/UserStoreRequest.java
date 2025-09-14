package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserStoreRequest {
    
    @NotBlank(message = "User ID không được để trống")
    private String userId;
    
    @NotBlank(message = "Store ID không được để trống")
    private String storeId;
}
