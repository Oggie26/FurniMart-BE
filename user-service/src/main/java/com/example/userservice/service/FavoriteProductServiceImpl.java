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

        // Kiểm tra user tồn tại
        userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (favoriteProductRepository.findByUserIdAndProductId(userId, request.getProductId()).isPresent()) {
            throw new AppException(ErrorCode.PRODUCT_ALREADY_FAVORITE);
        }
        FavoriteProduct favoriteProduct = FavoriteProduct.builder()
                .productId(request.getProductId())
                .userId(userId)
                .build();
        favoriteProductRepository.save(favoriteProduct);

        return toResponse(favoriteProduct);
    }

    @Override
    public void removeFavoriteProduct(String userId, String productId) {
        userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        FavoriteProduct favoriteProduct = favoriteProductRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.FAVORITE_PRODUCT_NOT_FOUND));
        favoriteProductRepository.delete(favoriteProduct);
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

