package com.example.userservice.service.inteface;

import com.example.userservice.request.FavoriteProductRequest;
import com.example.userservice.response.FavoriteProductResponse;
import com.example.userservice.response.PageResponse;

import java.util.List;

public interface FavoriteProductService {
    FavoriteProductResponse addFavoriteProduct(String userId, FavoriteProductRequest request);
    void removeFavoriteProduct(String userId, String productId);
    List<FavoriteProductResponse> getFavoriteProducts(String userId);
    PageResponse<FavoriteProductResponse> getFavoriteProductsWithPagination(String userId, int page, int size);
    boolean isFavoriteProduct(String userId, String productId);
    Long getFavoriteProductCount(String userId);
}

