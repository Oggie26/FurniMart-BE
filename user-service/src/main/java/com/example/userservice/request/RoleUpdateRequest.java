package com.example.userservice.request;

import com.example.userservice.enums.EnumRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleUpdateRequest {
    
    @NotNull(message = "Role cannot be null")
    private EnumRole role;
}
