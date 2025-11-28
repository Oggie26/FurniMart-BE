package com.example.userservice.service.inteface;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.request.StaffCreateCustomerRequest;
import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.ChangePassword;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.StaffCreateCustomerResponse;
import com.example.userservice.response.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserRequest userRequest);

    UserResponse updateUser(String id, UserUpdateRequest userRequest);

    UserResponse getUserById(String id);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUsersByStatus(String status);

    PageResponse<UserResponse> getUsersWithPagination(int page, int size);

    PageResponse<UserResponse> searchUsers(String searchTerm, int page, int size);

    void deleteUser(String id);

    void disableUser(String id);

    void enableUser(String id);

    UserResponse getProfile();

    UserResponse updateProfile(UserUpdateRequest request);

    void changePassword(ChangePassword changePassword);

    UserResponse getUserByEmail(String email);

    UserResponse getUserByPhone(String phone);

    UserResponse getUserByAccountId(String accountId);
    
    // Employee management methods (Admin only)
    List<UserResponse> getAllEmployees();
    
    List<UserResponse> getEmployeesByRole(EnumRole role);
    
    List<UserResponse> getEmployeesByStoreId(String storeId);
    
    List<UserResponse> getEmployeesByStoreIdAndRole(String storeId, EnumRole role);
    
    PageResponse<UserResponse> getEmployeesWithPagination(int page, int size);
    
    PageResponse<UserResponse> getEmployeesByRoleWithPagination(EnumRole role, int page, int size);
    
    // Role update method (Admin only)
    UserResponse updateUserRole(String userId, EnumRole newRole);
    
    // Staff method to create customer account with delivery address
    StaffCreateCustomerResponse createCustomerAccountForStaff(StaffCreateCustomerRequest request);
}
