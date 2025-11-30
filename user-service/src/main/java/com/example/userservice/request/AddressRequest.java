package com.example.userservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Phone is required")
    private String phone;

    @NotBlank(message = "City is required")
    private String city;

    private String district;

    @NotBlank(message = "Ward is required")
    private String ward;

    @NotBlank(message = "Street is required")
    private String street;

    private String addressLine;

    @NotNull(message = "Default flag is required")
    private Boolean isDefault;

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Latitude is required")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    private Double longitude;
}
