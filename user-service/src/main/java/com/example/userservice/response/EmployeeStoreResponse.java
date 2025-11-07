package com.example.userservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EmployeeStoreResponse {
    
    private String employeeId;  // Changed from userId to employeeId for clarity
    private String storeId;
    private UserResponse employee;  // Changed from user to employee for clarity
    private StoreResponse store;
    private Date createdAt;
    private Date updatedAt;
    
    // Backward compatibility: getter/setter for userId
    public String getUserId() {
        return employeeId;
    }
    
    public void setUserId(String userId) {
        this.employeeId = userId;
    }
    
    // Backward compatibility: getter/setter for user
    public UserResponse getUser() {
        return employee;
    }
    
    public void setUser(UserResponse user) {
        this.employee = user;
    }
}

