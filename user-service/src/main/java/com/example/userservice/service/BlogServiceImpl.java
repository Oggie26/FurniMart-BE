package com.example.userservice.service;

import com.example.userservice.entity.Blog;
import com.example.userservice.entity.User;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.BlogRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.BlogRequest;
import com.example.userservice.response.BlogResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.service.inteface.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BlogResponse createBlog(BlogRequest blogRequest) {
        log.info("Creating new blog with name: {}", blogRequest.getName());
        
        User user = userRepository.findById(blogRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Blog blog = Blog.builder()
                .name(blogRequest.getName())
                .content(blogRequest.getContent())
                .status(blogRequest.getStatus())
                .user(user)
                .image(blogRequest.getImage())
                .build();

        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog created successfully with ID: {}", savedBlog.getId());
        
        return toBlogResponse(savedBlog);
    }

    @Override
    @Transactional
    public BlogResponse updateBlog(Integer id, BlogRequest blogRequest) {
        log.info("Updating blog with ID: {}", id);
        
        Blog existingBlog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        User user = userRepository.findById(blogRequest.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        existingBlog.setName(blogRequest.getName());
        existingBlog.setContent(blogRequest.getContent());
        existingBlog.setStatus(blogRequest.getStatus());
        existingBlog.setImage(blogRequest.getImage());
        existingBlog.setUser(user);

        Blog updatedBlog = blogRepository.save(existingBlog);
        log.info("Blog updated successfully with ID: {}", updatedBlog.getId());
        
        return toBlogResponse(updatedBlog);
    }

    @Override
    public BlogResponse getBlogById(Integer id) {
        log.info("Fetching blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        return toBlogResponse(blog);
    }

    @Override
    public List<BlogResponse> getAllBlogs() {
        log.info("Fetching all blogs");
        
        List<Blog> blogs = blogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> getBlogsByUserId(String userId) {
        log.info("Fetching blogs for user ID: {}", userId);
        
        List<Blog> blogs = blogRepository.findByUserId(userId);
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> getBlogsByStatus(Boolean status) {
        log.info("Fetching blogs with status: {}", status);
        
        List<Blog> blogs = blogRepository.findByStatus(status);
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<BlogResponse> getBlogsWithPagination(int page, int size) {
        log.info("Fetching blogs with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Blog> blogPage = blogRepository.findAll(pageable);
        
        List<BlogResponse> blogResponses = blogPage.getContent().stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());

        return PageResponse.<BlogResponse>builder()
                .content(blogResponses)
                .totalElements(blogPage.getTotalElements())
                .totalPages(blogPage.getTotalPages())
                .size(blogPage.getSize())
                .number(blogPage.getNumber())
                .first(blogPage.isFirst())
                .last(blogPage.isLast())
                .build();
    }

    @Override
    public PageResponse<BlogResponse> getBlogsByUserWithPagination(String userId, int page, int size) {
        log.info("Fetching blogs for user ID: {} with pagination - page: {}, size: {}", userId, page, size);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Blog> blogPage = blogRepository.findByUser(user, pageable);
        
        List<BlogResponse> blogResponses = blogPage.getContent().stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());

        return PageResponse.<BlogResponse>builder()
                .content(blogResponses)
                .totalElements(blogPage.getTotalElements())
                .totalPages(blogPage.getTotalPages())
                .size(blogPage.getSize())
                .number(blogPage.getNumber())
                .first(blogPage.isFirst())
                .last(blogPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteBlog(Integer id) {
        log.info("Deleting blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setIsDeleted(true);
        blogRepository.save(blog);
        
        log.info("Blog soft deleted successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void toggleBlogStatus(Integer id) {
        log.info("Toggling status for blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        blog.setStatus(!blog.getStatus());
        blogRepository.save(blog);
        
        log.info("Blog status toggled successfully for ID: {}, new status: {}", id, blog.getStatus());
    }

    private BlogResponse toBlogResponse(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .name(blog.getName())
                .content(blog.getContent())
                .status(blog.getStatus())
                .userId(blog.getUser().getId())
                .image(blog.getImage())
                .userName(blog.getUser().getFullName())
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }
}
