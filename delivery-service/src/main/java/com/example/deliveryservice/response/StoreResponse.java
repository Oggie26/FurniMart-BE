package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {
    private String id;
    private String name;
    private String city;
    private String district;
    private String ward;
    private String street;
    private String addressLine;
    private Double latitude;
    private Double longitude;
    private EnumStatus status;
    private Date createdAt;
    private Date updatedAt;
}

