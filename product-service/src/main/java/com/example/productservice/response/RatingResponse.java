package com.example.productservice.response;

import com.example.productservice.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {
    private Long id;
    private String userId;
    private Product product;
    private Integer score;
    private String comment;
    private LocalDateTime createdAt;
}
