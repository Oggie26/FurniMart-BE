package com.example.productservice.service.inteface;

import com.example.productservice.entity.Product;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest productRequest);
    ProductResponse updateProduct(ProductRequest productRequest, String productId);
    void deleteProduct(String productId);
    void disableProduct(String productId);
    ProductResponse getProductById(String productId);
    ProductResponse getProductBySlug(String slug);
    List<ProductResponse> getProducts();
    List<Product> getProductsByCategoryId(Long categoryId);
    PageResponse<ProductResponse> searchProduct(String request, int page, int size);
}