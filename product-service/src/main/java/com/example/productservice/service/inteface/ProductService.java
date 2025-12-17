package com.example.productservice.service.inteface;

import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductQuickLookupResponse;
import com.example.productservice.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest);
    ProductResponse updateProduct(ProductRequest productRequest, String productId);
    void deleteProduct(String productId);
    void disableProduct(String productId);
    ProductResponse getProductById(String productId);
    ProductResponse getProductBySlug(String slug);
    List<ProductResponse> getProducts();
    List<ProductResponse> getProductsByCategoryId(Long categoryId);
    PageResponse<ProductResponse> searchProduct(String request, int page, int size);
    List<ProductQuickLookupResponse> quickLookup(String keyword);
}