package com.example.userservice.repository;

import com.example.userservice.entity.Blog;
import com.example.userservice.entity.Employee;
import com.example.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BlogRepository extends JpaRepository<Blog, Integer> {

    List<Blog> findByUser(User user);
    
    List<Blog> findByStatus(Boolean status);
    
    List<Blog> findByUserAndStatus(User user, Boolean status);
    
    @Query("SELECT b FROM Blog b WHERE b.user.id = :userId")
    List<Blog> findByUserId(@Param("userId") String userId);
    
    @Query("SELECT b FROM Blog b WHERE b.user.id = :userId AND b.status = :status")
    List<Blog> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") Boolean status);
    
    Page<Blog> findByStatus(Boolean status, Pageable pageable);
    
    Page<Blog> findByEmployee(Employee employee, Pageable pageable);
}
