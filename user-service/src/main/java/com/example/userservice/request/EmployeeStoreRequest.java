package com.example.userservice.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@Schema(description = "Request to add employee to store")
public class EmployeeStoreRequest {
    
    @NotBlank(message = "Employee ID không được để trống")
    @Pattern(
        regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        message = "Employee ID phải là UUID hợp lệ (ví dụ: ef5ec40c-198f-4dfb-84dc-5bf86db68940)"
    )
    @Schema(description = "ID of the employee to add to store", example = "ef5ec40c-198f-4dfb-84dc-5bf86db68940", required = true)
    private String employeeId;
    
    @NotBlank(message = "Store ID không được để trống")
    @Pattern(
        regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        message = "Store ID phải là UUID hợp lệ (ví dụ: 8d46e317-0596-4413-81b6-1a526398b3d7)"
    )
    @Schema(description = "ID of the store", example = "8d46e317-0596-4413-81b6-1a526398b3d7", required = true)
    private String storeId;
}

