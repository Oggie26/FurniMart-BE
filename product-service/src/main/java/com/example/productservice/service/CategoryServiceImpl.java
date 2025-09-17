package com.example.productservice.service;

import com.example.productservice.entity.Category;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.request.CategoryRequest;
import com.example.productservice.response.CategoryResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.service.inteface.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest categoryRequest) {
        if (categoryRepository.findByCategoryNameAndIsDeletedFalse(categoryRequest.getCategoryName()).isPresent()) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        Category category = Category.builder()
                .categoryName(categoryRequest.getCategoryName())
                .description(categoryRequest.getDescription())
                .status(EnumStatus.ACTIVE)
                .image(categoryRequest.getImage())
                .build();

        Category saved = categoryRepository.save(category);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id ,CategoryRequest categoryRequest) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        categoryRepository.findByCategoryNameAndIsDeletedFalse(categoryRequest.getCategoryName())
                .filter(existing -> !existing.getId().equals(category.getId()))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.CATEGORY_EXISTED);
                });

        category.setCategoryName(categoryRequest.getCategoryName());
        category.setDescription(categoryRequest.getDescription());
        category.setImage(categoryRequest.getImage());
        category.setStatus(categoryRequest.getStatus());

        Category updated = categoryRepository.save(category);
        return mapToResponse(updated);
    }


    @Override
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setIsDeleted(true);
        category.setStatus(EnumStatus.DELETED);
        categoryRepository.delete(category);
    }

    @Override
    public void disableCategory(Long categoryId) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        if(category.getStatus().equals(EnumStatus.ACTIVE)) {
            category.setStatus(EnumStatus.INACTIVE);
        }else{
            category.setStatus(EnumStatus.ACTIVE);
        }
        categoryRepository.save(category);
    }

    @Override
    public List<CategoryResponse> getCategory() {
        return categoryRepository.findAll()
                .stream()
                .filter(category -> !category.getIsDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategoryById(Long categoryId) {
        Category category = categoryRepository.findByIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return mapToResponse(category);
    }

    @Override
    public PageResponse<CategoryResponse> searchCategories(String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Category> categoryPage = categoryRepository.searchByKeywordNative(keyword, pageable);

        List<CategoryResponse> data = categoryPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                categoryPage.getNumber(),
                categoryPage.getSize(),
                categoryPage.getTotalElements(),
                categoryPage.getTotalPages()
        );
    }

    private CategoryResponse mapToResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .image(category.getImage())
                .build();
    }
}
