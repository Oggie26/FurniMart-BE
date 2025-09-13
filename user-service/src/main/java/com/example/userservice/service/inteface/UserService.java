package com.example.userservice.service.inteface;

import com.example.userservice.request.UserRequest;
import com.example.userservice.request.UserUpdateRequest;
import com.example.userservice.response.ChangePassword;
import com.example.userservice.response.PageResponse;
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
}
