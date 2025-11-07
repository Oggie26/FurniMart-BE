package com.example.userservice.service.inteface;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.UserResponse;

import java.util.List;

public interface EmployeeService {

    /**
     * Create a new employee with employee role (BRANCH_MANAGER, DELIVERY, STAFF).
     * Throws exception if attempting to create ADMIN or CUSTOMER role.
     * 
     * @param userRequest The employee data
     * @return Created employee response
     * @throws com.example.userservice.exception.AppException if role is ADMIN or CUSTOMER
     */
    UserResponse createEmployee(UserRequest userRequest);

    /**
     * Create a new admin user with ADMIN role.
     * Only existing admin users can create other admin accounts.
     * 
     * @param userRequest The admin data (role will be forced to ADMIN)
     * @return Created admin response
     * @throws com.example.userservice.exception.AppException if validation fails
     */
    UserResponse createAdmin(UserRequest userRequest);

    /**
     * Update employee information
     * 
     * @param id Employee ID
     * @param userRequest Updated employee data
     * @return Updated employee response
     */
    UserResponse updateEmployee(String id, UserUpdateRequest userRequest);

    /**
     * Get employee by ID
     * 
     * @param id Employee ID
     * @return Employee response
     */
    UserResponse getEmployeeById(String id);

    /**
     * Get employee by account ID
     * 
     * @param accountId Account ID
     * @return Employee response
     */
    UserResponse getEmployeeByAccountId(String accountId);

    /**
     * Get all employees (all employee roles)
     * 
     * @return List of all employees
     */
    List<UserResponse> getAllEmployees();

    /**
     * Get employees by specific role
     * 
     * @param role Employee role (must be employee role, not ADMIN or CUSTOMER)
     * @return List of employees with specified role
     */
    List<UserResponse> getEmployeesByRole(EnumRole role);

    /**
     * Get employees by store ID
     * 
     * @param storeId Store ID
     * @return List of employees assigned to the store
     */
    List<UserResponse> getEmployeesByStoreId(String storeId);

    /**
     * Get employees by store ID and role
     * 
     * @param storeId Store ID
     * @param role Employee role
     * @return List of employees with specified role in the store
     */
    List<UserResponse> getEmployeesByStoreIdAndRole(String storeId, EnumRole role);

    /**
     * Get employees with pagination
     * 
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated employee response
     */
    PageResponse<UserResponse> getEmployeesWithPagination(int page, int size);

    /**
     * Get employees by role with pagination
     * 
     * @param role Employee role
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated employee response
     */
    PageResponse<UserResponse> getEmployeesByRoleWithPagination(EnumRole role, int page, int size);

    /**
     * Search employees by name, email, or phone
     * 
     * @param searchTerm Search term
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return Paginated search results
     */
    PageResponse<UserResponse> searchEmployees(String searchTerm, int page, int size);

    /**
     * Update employee role (only to another employee role)
     * 
     * @param userId Employee ID
     * @param newRole New employee role (must be employee role)
     * @return Updated employee response
     */
    UserResponse updateEmployeeRole(String userId, EnumRole newRole);

    /**
     * Soft delete employee
     * 
     * @param id Employee ID
     */
    void deleteEmployee(String id);

    /**
     * Disable employee account
     * 
     * @param id Employee ID
     */
    void disableEmployee(String id);

    /**
     * Enable employee account
     * 
     * @param id Employee ID
     */
    void enableEmployee(String id);

    /**
     * Assign employee to store
     * 
     * @param employeeId Employee ID
     * @param storeId Store ID
     */
    void assignEmployeeToStore(String employeeId, String storeId);

    /**
     * Remove employee from store
     * 
     * @param employeeId Employee ID
     * @param storeId Store ID
     */
    void removeEmployeeFromStore(String employeeId, String storeId);

    /**
     * Get employee count by role
     * 
     * @param role Employee role
     * @return Count of employees with specified role
     */
    Long getEmployeeCountByRole(EnumRole role);

    /**
     * Get total employee count
     * 
     * @return Total count of all employees
     */
    Long getTotalEmployeeCount();
}

