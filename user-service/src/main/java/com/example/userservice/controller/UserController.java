package com.example.userservice.controller;

import com.example.userservice.request.UserRequest;
import com.example.userservice.response.*;
import com.example.userservice.service.inteface.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Controller")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final UserService userService;

//    @PostMapping
//    @Operation(summary = "Tạo người dùng mới")
//    @ResponseStatus(HttpStatus.CREATED)
//    public ApiResponse<UserResponse> createUser(@Valid @RequestBody UserRequest request) {
//        return ApiResponse.<UserResponse>builder()
//                .status(HttpStatus.CREATED.value())
//                .message("Tạo người dùng thành công")
//                .data(userService.createUser(request))
//                .build();
//    }
//
//    @PutMapping("/{id}")
//    @Operation(summary = "Cập nhật thông tin người dùng")
//    public ApiResponse<UserResponse> updateUser(@RequestParam String id ,@Valid @RequestBody UserRequest request) {
//        return ApiResponse.<UserResponse>builder()
//                .status(HttpStatus.OK.value())
//                .message("Cập nhật người dùng thành công")
//                .data(userService.updateUser(id, request))
//                .build();
//    }
//
//    @PatchMapping("/{id}")
//    @Operation(summary = "Vô hiệu hóa người dùng (set INACTIVE)")
//    public ApiResponse<Void> disableUser(@PathVariable String id) {
//        userService.disableUser(id);
//        return ApiResponse.<Void>builder()
//                .status(HttpStatus.OK.value())
//                .message("Đã vô hiệu hóa người dùng")
//                .build();
//    }
//
//    @DeleteMapping("/{id}")
//    @Operation(summary = "Xoá người dùng mềm (isDeleted)")
//    public ApiResponse<Void> deleteUser(@PathVariable String id) {
//        userService.deleteUser(id);
//        return ApiResponse.<Void>builder()
//                .status(HttpStatus.OK.value())
//                .message("Xoá người dùng thành công")
//                .build();
//    }
//
//    @GetMapping("/info/{authId}")
//    @Operation(summary = "Lấy thông tin cơ bản Use để truyền đi")
//    public ApiResponse<UserInfo> getUserInfo(@PathVariable String authId) {
//        return ApiResponse.<UserInfo>builder()
//                .status(HttpStatus.OK.value())
//                .data(userService.getUserInfo(authId))
//                .message("Lấy info người dùng thành công")
//                .build();
//    }
//
//    @GetMapping()
//    @Operation(summary = "Lấy tất cả User")
//    public ApiResponse<List<UserResponse>> getUsers() {
//        return ApiResponse.<List<UserResponse>>builder()
//                .status(HttpStatus.OK.value())
//                .data(userService.getUsers())
//                .message("Lấy người dùng thành công")
//                .build();
//    }
//
//
//    @GetMapping("/search")
//    @Operation(summary = "Tìm kiếm người dùng theo nhiều tiêu chí")
//    public ApiResponse<PageResponse<UserResponse>> searchUsers(
//            @RequestParam(required = false) String request,
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size) {
//
//        PageResponse<UserResponse> users = userService.searchUsers(request, page, size);
//
//        return ApiResponse.<PageResponse<UserResponse>>builder()
//                .status(HttpStatus.OK.value())
//                .message("Tìm kiếm thành công")
//                .data(users)
//                .build();
//    }
//
//    @GetMapping("/profile")
//    @Operation(summary = "Lấy thông tin profile người dùng")
//    public ApiResponse<UserResponse> getProfile() {
//        return ApiResponse.<UserResponse>builder()
//                .status(HttpStatus.OK.value())
//                .data(userService.getProfile())
//                .message("Lấy thành công")
//                .build();
//    }
//
//    @GetMapping("/{userId}")
//    @Operation(summary = "Lấy thông tin profile người ")
//    public ApiResponse<UserResponse> getByUserId(@PathVariable String userId) {
//        return ApiResponse.<UserResponse>builder()
//                .status(HttpStatus.OK.value())
//                .data(userService.getUserById(userId))
//                .message("Lấy thành công")
//                .build();
//    }
//
//    @PutMapping("/updateProfile")
//    @Operation(summary = "Cập nhật thông tin Profile")
//    public ApiResponse<UserResponse> updateProfile(@Valid @RequestBody UserRequest request) {
//        return ApiResponse.<UserResponse>builder()
//                .status(HttpStatus.OK.value())
//                .message("Cập nhật profile thành công")
//                .data(userService.updateProfile(request))
//                .build();
//    }
//
//    @PatchMapping("/changePassword")
//    @Operation(summary = "Thay đổi password")
//    public ApiResponse<Void> changePassword(@RequestBody ChangePassword changePassword) {
//        userService.changePassword(changePassword);
//        return ApiResponse.<Void>builder()
//                .status(HttpStatus.OK.value())
//                .message("Thay đổi mật khẩu thành công")
//                .build();
//    }
}


