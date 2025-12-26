package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.Blog;
import com.example.userservice.entity.Employee;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.BlogRepository;
import com.example.userservice.repository.EmployeeRepository;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final EmployeeRepository employeeRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public BlogResponse createBlog(BlogRequest blogRequest) {
        log.info("Creating new blog with name: {}", blogRequest.getName());

        // 1. Lấy nhân viên đang đăng nhập (tự động từ authentication)
        Employee employee = getCurrentEmployee();
        
        // 2. Tạo Entity
        Blog blog = Blog.builder()
                .name(blogRequest.getName())
                .content(blogRequest.getContent())
                .status(blogRequest.getStatus())
                .image(blogRequest.getImage())
                .employee(employee)
                .build();

        // 3. Lưu và trả về
        Blog savedBlog = blogRepository.save(blog);
        log.info("Blog created successfully with ID: {} by employee: {}", savedBlog.getId(), employee.getId());

        return toBlogResponse(savedBlog);
    }


    @Override
    @Transactional
    public BlogResponse updateBlog(Integer id, BlogRequest blogRequest) {
        log.info("Updating blog with ID: {}", id);

        Employee currentEmployee = getCurrentEmployee();
        Blog existingBlog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Check hierarchical authorization
        Employee blogOwner = existingBlog.getEmployee();
        if (!canManageBlog(currentEmployee, blogOwner)) {
            log.warn("Employee {} attempted to update blog {} owned by {}", 
                    currentEmployee.getId(), id, blogOwner.getId());
            throw new AppException(ErrorCode.BLOG_HIERARCHY_DENIED);
        }

        existingBlog.setName(blogRequest.getName());
        existingBlog.setContent(blogRequest.getContent());
        existingBlog.setStatus(blogRequest.getStatus());
        // Don't change employee - blog ownership should not change on update
        if (blogRequest.getImage() != null && !blogRequest.getImage().isEmpty()) {
            existingBlog.setImage(blogRequest.getImage());
        }

        Blog updatedBlog = blogRepository.save(existingBlog);
        log.info("Blog updated successfully with ID: {} by employee: {}", updatedBlog.getId(), currentEmployee.getId());

        return toBlogResponse(updatedBlog);
    }

    @Override
    public BlogResponse getBlogById(Integer id) {
        log.info("Fetching blog with ID: {}", id);
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));
        
        // Filter by status if user is CUSTOMER or DELIVERY
        if (shouldFilterByStatus() && !blog.getStatus()) {
            log.warn("User attempted to access unpublished blog {}", id);
            throw new AppException(ErrorCode.BLOG_ACCESS_DENIED);
        }
        
        return toBlogResponse(blog);
    }

    @Override
    public List<BlogResponse> getAllBlogs() {
        log.info("Fetching all blogs");
        
        List<Blog> blogs = blogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        
        // Filter by status if user is CUSTOMER or DELIVERY
        if (shouldFilterByStatus()) {
            blogs = blogs.stream()
                    .filter(blog -> blog.getStatus())
                    .collect(Collectors.toList());
        }
        
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> getBlogsByUserId(String userId) {
        log.info("Fetching blogs for user ID: {}", userId);
        
        List<Blog> blogs = blogRepository.findByEmployeeId(userId);
        
        // Filter by status if user is CUSTOMER or DELIVERY
        if (shouldFilterByStatus()) {
            blogs = blogs.stream()
                    .filter(blog -> blog.getStatus())
                    .collect(Collectors.toList());
        }
        
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<BlogResponse> getBlogsByStatus(Boolean status) {
        log.info("Fetching blogs with status: {}", status);
        
        // If user is CUSTOMER or DELIVERY, they can only see published blogs
        if (shouldFilterByStatus() && !status) {
            log.warn("User attempted to access unpublished blogs");
            throw new AppException(ErrorCode.BLOG_ACCESS_DENIED);
        }
        
        List<Blog> blogs = blogRepository.findByStatus(status);
        return blogs.stream()
                .map(this::toBlogResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PageResponse<BlogResponse> getBlogsWithPagination(int page, int size) {
        log.info("Fetching blogs with pagination - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Blog> blogPage;
        
        // Filter by status if user is CUSTOMER or DELIVERY
        if (shouldFilterByStatus()) {
            blogPage = blogRepository.findByStatus(true, pageable);
        } else {
            blogPage = blogRepository.findAll(pageable);
        }
        
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
        
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Blog> blogPage;
        
        // Filter by status if user is CUSTOMER or DELIVERY
        if (shouldFilterByStatus()) {
            blogPage = blogRepository.findByEmployeeAndStatus(employee, true, pageable);
        } else {
            blogPage = blogRepository.findByEmployee(employee, pageable);
        }
        
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
        
        Employee currentEmployee = getCurrentEmployee();
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Check hierarchical authorization
        Employee blogOwner = blog.getEmployee();
        if (!canManageBlog(currentEmployee, blogOwner)) {
            log.warn("Employee {} attempted to delete blog {} owned by {}", 
                    currentEmployee.getId(), id, blogOwner.getId());
            throw new AppException(ErrorCode.BLOG_HIERARCHY_DENIED);
        }

        blog.setIsDeleted(true);
        blogRepository.save(blog);
        
        log.info("Blog soft deleted successfully with ID: {} by employee: {}", id, currentEmployee.getId());
    }

    @Override
    @Transactional
    public void toggleBlogStatus(Integer id) {
        log.info("Toggling status for blog with ID: {}", id);
        
        Employee currentEmployee = getCurrentEmployee();
        EnumRole currentRole = getCurrentEmployeeRole();
        
        // STAFF cannot toggle status
        if (currentRole == EnumRole.STAFF) {
            log.warn("STAFF {} attempted to toggle status of blog {}", currentEmployee.getId(), id);
            throw new AppException(ErrorCode.BLOG_TOGGLE_STATUS_DENIED);
        }
        
        Blog blog = blogRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_FOUND));

        // Check hierarchical authorization
        Employee blogOwner = blog.getEmployee();
        if (!canManageBlog(currentEmployee, blogOwner)) {
            log.warn("Employee {} attempted to toggle status of blog {} owned by {}", 
                    currentEmployee.getId(), id, blogOwner.getId());
            throw new AppException(ErrorCode.BLOG_HIERARCHY_DENIED);
        }

        blog.setStatus(!blog.getStatus());
        blogRepository.save(blog);
        
        log.info("Blog status toggled successfully for ID: {}, new status: {} by employee: {}", 
                id, blog.getStatus(), currentEmployee.getId());
    }

    private BlogResponse toBlogResponse(Blog blog) {
        return BlogResponse.builder()
                .id(blog.getId())
                .name(blog.getName())
                .content(blog.getContent())
                .status(blog.getStatus())
                .employeeId(blog.getEmployee() != null ? blog.getEmployee().getId() : null)
                .image(blog.getImage())
                .employeeName(blog.getEmployee() != null ? blog.getEmployee().getFullName() : "FurniMart-BE")
                .createdAt(blog.getCreatedAt())
                .updatedAt(blog.getUpdatedAt())
                .build();
    }

    /**
     * Get current employee from authentication context
     */
    private Employee getCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        
        String email = authentication.getName();
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
        
        if (account.getEmployee() == null) {
            log.error("Account {} does not have an associated employee", account.getId());
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        return account.getEmployee();
    }

    /**
     * Get current employee role from authentication context
     */
    private EnumRole getCurrentEmployeeRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        String email = authentication.getName();
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElse(null);
        
        return account != null ? account.getRole() : null;
    }

    /**
     * Check if current employee can manage blog of blogOwner
     * Hierarchical authorization: cấp trên có toàn quyền với cấp dưới, không có quyền với đồng cấp hoặc cấp trên
     */
    private boolean canManageBlog(Employee currentEmployee, Employee blogOwner) {
        // Everyone can manage their own blog
        if (currentEmployee.getId().equals(blogOwner.getId())) {
            return true;
        }
        
        // Get roles
        EnumRole currentRole = currentEmployee.getAccount().getRole();
        EnumRole ownerRole = blogOwner.getAccount().getRole();
        
        // ADMIN can manage blog of BRANCH_MANAGER and STAFF (subordinates)
        if (currentRole == EnumRole.ADMIN) {
            return ownerRole == EnumRole.BRANCH_MANAGER || ownerRole == EnumRole.STAFF;
        }
        
        // BRANCH_MANAGER can manage blog of STAFF (subordinates)
        if (currentRole == EnumRole.BRANCH_MANAGER) {
            return ownerRole == EnumRole.STAFF;
        }
        
        // STAFF can only manage their own blog (already checked above)
        return false;
    }

    /**
     * Check if should filter blogs by status (only show published blogs)
     * Returns true for CUSTOMER and DELIVERY roles, or if user is not authenticated
     */
    private boolean shouldFilterByStatus() {
        EnumRole role = getCurrentEmployeeRole();
        // If not authenticated or role is CUSTOMER/DELIVERY, filter by status
        if (role == null) {
            return true; // Not authenticated - only show published blogs
        }
        return role == EnumRole.CUSTOMER || role == EnumRole.DELIVERY;
    }
}
