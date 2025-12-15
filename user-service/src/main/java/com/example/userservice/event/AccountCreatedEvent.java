package com.example.userservice.event;

import com.example.userservice.enums.EnumRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountCreatedEvent {
    private String id;
    private String fullName;
    private String email;
    private EnumRole role;
}