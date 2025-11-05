package com.example.userservice.service;

import com.example.userservice.entity.FavoriteProduct;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.FavoriteProductRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.FavoriteProductRequest;
import com.example.userservice.response.FavoriteProductResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.FavoriteProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FavoriteProductServiceImpl implements FavoriteProductService {

    private final FavoriteProductRepository favoriteProductRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public FavoriteProductResponse addFavoriteProduct(String userId, FavoriteProductRequest request) {
        log.info("Adding favorite product {} for user {}", request.getProductId(), userId);

        // Verify user exists
        userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Check if already favorite
        if (favoriteProductRepository.existsByUserIdAndProductIdAndIsDeletedFalse(userId, request.getProductId())) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_FAVORITE);
        }

        FavoriteProduct favoriteProduct = FavoriteProduct.builder()
                .userId(userId)
                .productId(request.getProductId())
                .build();

        FavoriteProduct saved = favoriteProductRepository.save(favoriteProduct);
        log.info("Added favorite product {} for user {}", request.getProductId(), userId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void removeFavoriteProduct(String userId, String productId) {
        log.info("Removing favorite product {} for user {}", productId, userId);

        FavoriteProduct favoriteProduct = favoriteProductRepository
                .findByUserIdAndProductIdAndIsDeletedFalse(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_PRODUCT_NOT_FOUND));

        favoriteProduct.setIsDeleted(true);
        favoriteProductRepository.save(favoriteProduct);
        log.info("Removed favorite product {} for user {}", productId, userId);
    }

    @Override
    public List<FavoriteProductResponse> getFavoriteProducts(String userId) {
        log.info("Fetching favorite products for user {}", userId);

        List<FavoriteProduct> favorites = favoriteProductRepository.findByUserIdAndIsDeletedFalse(userId);
        return favorites.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<FavoriteProductResponse> getFavoriteProductsWithPagination(String userId, int page, int size) {
        log.info("Fetching favorite products for user {} with pagination - page: {}, size: {}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FavoriteProduct> favoritePage = favoriteProductRepository.findByUserIdAndIsDeletedFalse(userId, pageable);

        List<FavoriteProductResponse> responses = favoritePage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return PageResponse.<FavoriteProductResponse>builder()
                .content(responses)
                .totalElements(favoritePage.getTotalElements())
                .totalPages(favoritePage.getTotalPages())
                .size(favoritePage.getSize())
                .number(favoritePage.getNumber())
                .first(favoritePage.isFirst())
                .last(favoritePage.isLast())
                .build();
    }

    @Override
    public boolean isFavoriteProduct(String userId, String productId) {
        return favoriteProductRepository.existsByUserIdAndProductIdAndIsDeletedFalse(userId, productId);
    }

    @Override
    public Long getFavoriteProductCount(String userId) {
        return favoriteProductRepository.countByUserId(userId);
    }

    private FavoriteProductResponse toResponse(FavoriteProduct favoriteProduct) {
        return FavoriteProductResponse.builder()
                .id(favoriteProduct.getId())
                .userId(favoriteProduct.getUserId())
                .productId(favoriteProduct.getProductId())
                .createdAt(favoriteProduct.getCreatedAt())
                .build();
    }
}

