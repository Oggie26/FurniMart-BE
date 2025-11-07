package com.example.userservice.repository;

import com.example.userservice.entity.Employee;
import com.example.userservice.enums.EnumRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /**
     * Find employee by ID (excludes CUSTOMER roles)
     */
    @Query("SELECT e FROM Employee e WHERE e.id = :id AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Optional<Employee> findEmployeeById(@Param("id") String id);

    /**
     * Find all employees (excludes CUSTOMER roles)
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    List<Employee> findAllEmployees();

    /**
     * Find all employees with pagination
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Page<Employee> findAllEmployees(Pageable pageable);

    /**
     * Find employees by specific role
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role = :role AND e.isDeleted = false")
    List<Employee> findEmployeesByRole(@Param("role") EnumRole role);

    /**
     * Find employees by specific role with pagination
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role = :role AND e.isDeleted = false")
    Page<Employee> findEmployeesByRole(@Param("role") EnumRole role, Pageable pageable);

    /**
     * Find employees by store ID
     */
    @Query("SELECT e FROM Employee e JOIN e.employeeStores es WHERE es.storeId = :storeId AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    List<Employee> findEmployeesByStoreId(@Param("storeId") String storeId);

    /**
     * Find employees by store ID and role
     */
    @Query("SELECT e FROM Employee e JOIN e.employeeStores es WHERE es.storeId = :storeId AND e.account.role = :role AND e.isDeleted = false")
    List<Employee> findEmployeesByStoreIdAndRole(@Param("storeId") String storeId, @Param("role") EnumRole role);

    /**
     * Find employees by multiple roles
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role IN :roles AND e.isDeleted = false")
    List<Employee> findEmployeesByRoles(@Param("roles") List<EnumRole> roles);

    /**
     * Find employees by multiple roles with pagination
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role IN :roles AND e.isDeleted = false")
    Page<Employee> findEmployeesByRoles(@Param("roles") List<EnumRole> roles, Pageable pageable);

    /**
     * Search employees by name, email, or phone
     */
    @Query("SELECT e FROM Employee e WHERE e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false AND " +
           "(LOWER(e.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(e.account.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Employee> searchEmployees(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Count employees by role
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.account.role = :role AND e.isDeleted = false")
    Long countEmployeesByRole(@Param("role") EnumRole role);

    /**
     * Count all employees
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Long countAllEmployees();

    /**
     * Check if employee exists by email
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.account.email = :email AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    boolean existsEmployeeByEmail(@Param("email") String email);

    /**
     * Check if employee exists by phone
     */
    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Employee e WHERE e.phone = :phone AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    boolean existsEmployeeByPhone(@Param("phone") String phone);

    /**
     * Find employee by phone (excludes CUSTOMER roles)
     */
    @Query("SELECT e FROM Employee e WHERE e.phone = :phone AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Optional<Employee> findByPhoneAndIsDeletedFalse(@Param("phone") String phone);

    /**
     * Find employee by email (excludes CUSTOMER roles)
     */
    @Query("SELECT e FROM Employee e WHERE e.account.email = :email AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Optional<Employee> findByEmailAndIsDeletedFalse(@Param("email") String email);

    /**
     * Find employee by account ID (excludes CUSTOMER roles)
     */
    @Query("SELECT e FROM Employee e WHERE e.account.id = :accountId AND e.account.role IN ('BRANCH_MANAGER', 'DELIVERY', 'STAFF', 'ADMIN') AND e.isDeleted = false")
    Optional<Employee> findByAccountIdAndIsDeletedFalse(@Param("accountId") String accountId);
}

