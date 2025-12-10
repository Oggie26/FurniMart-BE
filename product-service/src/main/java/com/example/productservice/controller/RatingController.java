package com.example.productservice.controller;

import com.example.productservice.request.RatingRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.RatingResponse;
import com.example.productservice.service.inteface.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ratings")
@Tag(name = "Rating Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @Operation(summary = "Tạo đánh giá cho sản phẩm")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RatingResponse> createRating(@Valid @RequestBody RatingRequest ratingRequest) {
        return ApiResponse.<RatingResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo đánh giá thành công")
                .data(ratingService.createRating(ratingRequest))
                .build();
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Lấy danh sách đánh giá của sản phẩm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<RatingResponse>> getRatingsByProduct(@PathVariable String productId) {
        return ApiResponse.<List<RatingResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách đánh giá thành công")
                .data(ratingService.getRatingsByProduct(productId))
                .build();
    }

    @GetMapping("/product/{productId}/average")
    @Operation(summary = "Lấy điểm đánh giá trung bình của sản phẩm")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Double> getAverageRating(@PathVariable String productId) {
        return ApiResponse.<Double>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy điểm trung bình thành công")
                .data(ratingService.getAverageRating(productId))
                .build();
    }

    @DeleteMapping("/{ratingId}")
    @Operation(summary = "Xóa đánh giá")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteRating(@PathVariable Long ratingId) {
        ratingService.deleteRating(ratingId);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xóa đánh giá thành công")
                .build();
    }
}
