package com.example.productservice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {

    @NotBlank
    private String userId; // id của customer

    @NotBlank
    private String productId; // id sản phẩm

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score; // đánh giá từ 1-5

    private String comment; // bình luận, không bắt buộc
}
