package com.example.userservice.repository;

import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for employee-specific database operations.
 * Handles queries for users with employee roles (MANAGER, DELIVERY, STAFF).
 * Excludes ADMIN and CUSTOMER roles from employee operations.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<User, String> {

    /**
     * Find employee by ID (excludes ADMIN and CUSTOMER roles)
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    Optional<User> findEmployeeById(@Param("id") String id);

    /**
     * Find all employees (excludes ADMIN and CUSTOMER roles)
     */
    @Query("SELECT u FROM User u WHERE u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    List<User> findAllEmployees();

    /**
     * Find all employees with pagination
     */
    @Query("SELECT u FROM User u WHERE u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    Page<User> findAllEmployees(Pageable pageable);

    /**
     * Find employees by specific role
     */
    @Query("SELECT u FROM User u WHERE u.account.role = :role AND u.isDeleted = false")
    List<User> findEmployeesByRole(@Param("role") EnumRole role);

    /**
     * Find employees by specific role with pagination
     */
    @Query("SELECT u FROM User u WHERE u.account.role = :role AND u.isDeleted = false")
    Page<User> findEmployeesByRole(@Param("role") EnumRole role, Pageable pageable);

    /**
     * Find employees by store ID
     */
    @Query("SELECT u FROM User u JOIN u.userStores us WHERE us.storeId = :storeId AND u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    List<User> findEmployeesByStoreId(@Param("storeId") String storeId);

    /**
     * Find employees by store ID and role
     */
    @Query("SELECT u FROM User u JOIN u.userStores us WHERE us.storeId = :storeId AND u.account.role = :role AND u.isDeleted = false")
    List<User> findEmployeesByStoreIdAndRole(@Param("storeId") String storeId, @Param("role") EnumRole role);

    /**
     * Find employees by multiple roles
     */
    @Query("SELECT u FROM User u WHERE u.account.role IN :roles AND u.isDeleted = false")
    List<User> findEmployeesByRoles(@Param("roles") List<EnumRole> roles);

    /**
     * Find employees by multiple roles with pagination
     */
    @Query("SELECT u FROM User u WHERE u.account.role IN :roles AND u.isDeleted = false")
    Page<User> findEmployeesByRoles(@Param("roles") List<EnumRole> roles, Pageable pageable);

    /**
     * Search employees by name, email, or phone
     */
    @Query("SELECT u FROM User u WHERE u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.account.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchEmployees(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count employees by role
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.account.role = :role AND u.isDeleted = false")
    Long countEmployeesByRole(@Param("role") EnumRole role);

    /**
     * Count all employees
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    Long countAllEmployees();

    /**
     * Check if employee exists by email
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.account.email = :email AND u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    boolean existsEmployeeByEmail(@Param("email") String email);

    /**
     * Check if employee exists by phone
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.phone = :phone AND u.account.role IN ('MANAGER', 'DELIVERY', 'STAFF') AND u.isDeleted = false")
    boolean existsEmployeeByPhone(@Param("phone") String phone);
}

