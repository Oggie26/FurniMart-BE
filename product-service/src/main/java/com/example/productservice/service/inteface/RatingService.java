package com.example.productservice.service.inteface;

import com.example.productservice.request.RatingRequest;
import com.example.productservice.response.RatingResponse;

import java.util.List;

public interface RatingService {
    RatingResponse createRating(RatingRequest request,Long orderId);
    List<RatingResponse> getRatingsByProduct(String productId);
    double getAverageRating(String productId);
    void deleteRating(Long ratingId);
}
