package com.example.inventoryservice.response;
import com.example.inventoryservice.enums.EnumRole;
import com.example.inventoryservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class UserInfo {
    String fullName;
    @Enumerated(EnumType.STRING)
    EnumStatus status;
    @Enumerated(EnumType.STRING)
    EnumRole role;
    Boolean isDeleted;
}
