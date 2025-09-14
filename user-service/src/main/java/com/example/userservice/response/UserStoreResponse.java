package com.example.userservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserStoreResponse {
    
    private String userId;
    private String storeId;
    private UserResponse user;
    private StoreResponse store;
    private Date createdAt;
    private Date updatedAt;
}
