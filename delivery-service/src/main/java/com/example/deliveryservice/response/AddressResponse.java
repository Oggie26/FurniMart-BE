package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private Long id;
    private String name;
    private String phone;
    private String city;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private Boolean isDefault;
    private String userId;
    private String userName;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
}

