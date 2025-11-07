package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class EmployeeStoreRequest {
    
    @NotBlank(message = "Employee ID không được để trống")
    private String employeeId;  // Changed from userId to employeeId for clarity
    
    @NotBlank(message = "Store ID không được để trống")
    private String storeId;
    
    // Backward compatibility: getter/setter for userId
    public String getUserId() {
        return employeeId;
    }
    
    public void setUserId(String userId) {
        this.employeeId = userId;
    }
}

