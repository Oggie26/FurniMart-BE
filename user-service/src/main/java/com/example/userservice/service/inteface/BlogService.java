package com.example.userservice.service.inteface;

import com.example.userservice.request.BlogRequest;
import com.example.userservice.response.BlogResponse;
import com.example.userservice.response.PageResponse;

import java.util.List;

public interface BlogService {

    BlogResponse createBlog(BlogRequest blogRequest);

    BlogResponse updateBlog(Integer id, BlogRequest blogRequest);

    BlogResponse getBlogById(Integer id);

    List<BlogResponse> getAllBlogs();

    List<BlogResponse> getBlogsByUserId(String userId);

    List<BlogResponse> getBlogsByStatus(Boolean status);

    PageResponse<BlogResponse> getBlogsWithPagination(int page, int size);

    PageResponse<BlogResponse> getBlogsByUserWithPagination(String userId, int page, int size);

    void deleteBlog(Integer id);

    void toggleBlogStatus(Integer id);
}
