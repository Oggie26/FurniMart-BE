package com.example.userservice.response;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
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
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
    
    private Date createdAt;
    private Date updatedAt;
    
    private List<UserResponse> users;
}
