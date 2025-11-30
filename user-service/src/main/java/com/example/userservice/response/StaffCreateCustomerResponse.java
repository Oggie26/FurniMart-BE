package com.example.userservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCreateCustomerResponse {
    private UserResponse user;
    private AddressResponse address;
    private String generatedPassword; // Only included if password was auto-generated
}

