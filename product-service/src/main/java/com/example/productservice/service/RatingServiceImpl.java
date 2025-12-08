package com.example.productservice.service;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.Rating;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.repository.RatingRepository;
import com.example.productservice.request.RatingRequest;
import com.example.productservice.response.RatingResponse;
import com.example.productservice.service.inteface.RatingService;
import com.example.productservice.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final ProductRepository productRepository;

    @Override
    public RatingResponse createRating(RatingRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Rating rating = Rating.builder()
                .userId(request.getUserId())
                .product(product)
                .score(request.getScore())
                .comment(request.getComment())
                .createdAt(new Date())
                .build();

        Rating saved = ratingRepository.save(rating);
        return mapToRatingResponse(saved);
    }

    @Override
    public List<RatingResponse> getRatingsByProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<Rating> ratings = ratingRepository.findByProduct(product);
        return ratings.stream()
                .map(this::mapToRatingResponse)
                .collect(Collectors.toList());
    }

    @Override
    public double getAverageRating(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<Rating> ratings = ratingRepository.findByProduct(product);
        if (ratings.isEmpty()) return 0;

        return ratings.stream()
                .mapToInt(Rating::getScore)
                .average()
                .orElse(0);
    }

    @Override
    public void deleteRating(Long ratingId) {
        Rating rating = ratingRepository.findById(ratingId)
                .orElseThrow(() -> new AppException(ErrorCode.RATING_NOT_FOUND));
        ratingRepository.delete(rating);
    }

    private RatingResponse mapToRatingResponse(Rating rating) {
        return RatingResponse.builder()
                .id(rating.getId())
                .userId(rating.getUserId())
                .product(rating.getProduct())
                .score(rating.getScore())
                .comment(rating.getComment())
                .createdAt(rating.getCreatedAt())
                .build();
    }
}
