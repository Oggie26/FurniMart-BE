package com.example.productservice.service.inteface;

import com.example.productservice.request.ProductColorRequest;
import com.example.productservice.response.MaterialResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductColorResponse;

import java.util.List;

public interface ProductColorService {
    ProductColorResponse addProductColor(ProductColorRequest productColorRequest);
    ProductColorResponse updateProductColor(ProductColorRequest productColorRequest, String id);
    void deleteProductColor(String productColorId);
    void disableProductColor(String productColorId);
    List<ProductColorResponse> getProductColors();
    ProductColorResponse getProductColor(String productColorId);
}
