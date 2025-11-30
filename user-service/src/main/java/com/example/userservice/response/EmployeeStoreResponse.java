package com.example.userservice.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Schema(description = "Response for employee-store relationship")
public class EmployeeStoreResponse {
    
    @Schema(description = "Employee ID", example = "ef5ec40c-198f-4dfb-84dc-5bf86db68940", required = true)
    private String employeeId;
    
    @Schema(description = "Store ID", example = "8d46e317-0596-4413-81b6-1a526398b3d7", required = true)
    private String storeId;
    
    @Schema(description = "Employee information", required = true)
    private UserResponse employee;
    
    @Schema(description = "Store information", required = true)
    private StoreResponse store;
    
    @Schema(description = "Creation timestamp")
    private Date createdAt;
    
    @Schema(description = "Last update timestamp")
    private Date updatedAt;
    
    // Backward compatibility: getter/setter for userId (hidden from Swagger)
    @JsonIgnore
    @Schema(hidden = true)
    public String getUserId() {
        return employeeId;
    }
    
    @JsonIgnore
    @Schema(hidden = true)
    public void setUserId(String userId) {
        this.employeeId = userId;
    }
    
    // Backward compatibility: getter/setter for user (hidden from Swagger)
    @JsonIgnore
    @Schema(hidden = true)
    public UserResponse getUser() {
        return employee;
    }
    
    @JsonIgnore
    @Schema(hidden = true)
    public void setUser(UserResponse user) {
        this.employee = user;
    }
}

