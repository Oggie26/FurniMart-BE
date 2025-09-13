package com.example.productservice.controller;

import com.example.productservice.request.CategoryRequest;
import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.CategoryResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.service.inteface.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @Operation(summary = "Tạo danh mục mới")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Tạo danh mục thành công")
                .data(categoryService.createCategory(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật danh mục")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CategoryResponse> updateCategory(@RequestParam Long id,@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Cập nhật danh mục thành công")
                .data(categoryService.updateCategory(id,request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá mềm danh mục (isDeleted)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Xoá danh mục thành công")
                .build();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Vô hiệu hoá danh mục (set INACTIVE)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> disableCategory(@PathVariable Long id) {
        categoryService.disableCategory(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Đã vô hiệu hoá danh mục")
                .build();
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả danh mục")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<List<CategoryResponse>> getCategories() {
        return ApiResponse.<List<CategoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh sách danh mục thành công")
                .data(categoryService.getCategory())
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết danh mục theo ID")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ApiResponse.<CategoryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy danh mục thành công")
                .data(categoryService.getCategoryById(id))
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm người dùng theo nhiều tiêu chí")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<PageResponse<CategoryResponse>> searchUsers(
            @RequestParam(required = false) String request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageResponse<CategoryResponse> users = categoryService.searchCategories(request, page, size);

        return ApiResponse.<PageResponse<CategoryResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Tìm kiếm thành công")
                .data(users)
                .build();
    }
}
