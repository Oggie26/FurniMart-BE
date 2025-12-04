package com.example.userservice.repository;

import com.example.userservice.entity.Blog;
import com.example.userservice.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Integer> {

    // 1. Tìm theo status
    List<Blog> findByStatus(Boolean status);

    List<Blog> findByEmployeeAndStatus(Employee employee, Boolean status);

    @Query("SELECT b FROM Blog b WHERE b.employee.id = :employeeId")
    List<Blog> findByEmployeeId(@Param("employeeId") String employeeId);

    @Query("SELECT b FROM Blog b WHERE b.employee.id = :employeeId AND b.status = :status")
    List<Blog> findByEmployeeIdAndStatus(@Param("employeeId") String employeeId, @Param("status") Boolean status);

    // 5. Phân trang
    Page<Blog> findByStatus(Boolean status, Pageable pageable);

    // 6. Phân trang theo Employee
    Page<Blog> findByEmployee(Employee employee, Pageable pageable);
}