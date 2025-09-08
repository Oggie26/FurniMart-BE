package com.example.productservice.service.inteface;

import com.example.productservice.request.CategoryRequest;
import com.example.productservice.response.CategoryResponse;
import com.example.productservice.response.PageResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse updateCategory(Long id ,CategoryRequest categoryRequest);
    void deleteCategory( Long categoryId);
    void disableCategory(Long categoryId);
    List<CategoryResponse> getCategory();
    CategoryResponse getCategoryById(Long categoryId);
    PageResponse<CategoryResponse> searchCategories(String request, int page, int size);

}
