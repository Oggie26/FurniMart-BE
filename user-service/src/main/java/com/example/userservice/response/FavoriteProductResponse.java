package com.example.userservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteProductResponse {
    private Long id;
    private String userId;
    private String productId;
    private Date createdAt;
}

