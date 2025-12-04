package com.example.userservice.controller;

import com.example.userservice.request.BlogRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.BlogResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.BlogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/blogs")
@Tag(name = "Blog Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    @Operation(summary = "Create a new blog")
    // Allow all authenticated roles to create blogs (ADMIN, BRANCH_MANAGER, STAFF, DELIVERY, CUSTOMER)
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER') or hasRole('STAFF') or hasRole('DELIVERY') or hasRole('CUSTOMER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BlogResponse> createBlog(@Valid @RequestBody BlogRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Blog created successfully")
                .data(blogService.createBlog(request))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update blog information")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<BlogResponse> updateBlog(@PathVariable Integer id, @Valid @RequestBody BlogRequest request) {
        return ApiResponse.<BlogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Blog updated successfully")
                .data(blogService.updateBlog(id, request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get blog by ID")
    public ApiResponse<BlogResponse> getBlogById(@PathVariable Integer id) {
        return ApiResponse.<BlogResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Blog retrieved successfully")
                .data(blogService.getBlogById(id))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all blogs")
    public ApiResponse<List<BlogResponse>> getAllBlogs() {
        return ApiResponse.<List<BlogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Blogs retrieved successfully")
                .data(blogService.getAllBlogs())
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get blogs by user ID")
    public ApiResponse<List<BlogResponse>> getBlogsByUserId(@PathVariable String userId) {
        return ApiResponse.<List<BlogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("User blogs retrieved successfully")
                .data(blogService.getBlogsByUserId(userId))
                .build();
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get blogs by status")
    public ApiResponse<List<BlogResponse>> getBlogsByStatus(@PathVariable Boolean status) {
        return ApiResponse.<List<BlogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Blogs by status retrieved successfully")
                .data(blogService.getBlogsByStatus(status))
                .build();
    }

    @GetMapping("/paginated")
    @Operation(summary = "Get blogs with pagination")
    public ApiResponse<PageResponse<BlogResponse>> getBlogsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Blogs retrieved successfully with pagination")
                .data(blogService.getBlogsWithPagination(page, size))
                .build();
    }

    @GetMapping("/user/{userId}/paginated")
    @Operation(summary = "Get user blogs with pagination")
    public ApiResponse<PageResponse<BlogResponse>> getBlogsByUserWithPagination(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<BlogResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("User blogs retrieved successfully with pagination")
                .data(blogService.getBlogsByUserWithPagination(userId, page, size))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete blog")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<Void> deleteBlog(@PathVariable Integer id) {
        blogService.deleteBlog(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Blog deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(summary = "Toggle blog status")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<Void> toggleBlogStatus(@PathVariable Integer id) {
        blogService.toggleBlogStatus(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Blog status toggled successfully")
                .build();
    }
}
