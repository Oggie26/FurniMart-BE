package com.example.productservice.service.inteface;

import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest);
    ProductResponse updateProduct(ProductRequest productRequest, String productId);
    void deleteProduct(String productId);
    void disableProduct(String productId);
    ProductResponse getProduct(String productId);
    List<ProductResponse> getProducts();
    PageResponse<ProductResponse> searchProduct(String request, int page, int size);
}