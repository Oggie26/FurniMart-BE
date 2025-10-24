package com.example.userservice.request;

import com.example.userservice.enums.EnumStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

/**
 * Request DTO for creating admin users.
 * This is a specialized request for admin creation with additional security validations.
 */
@Builder
@Data
public class AdminRequest {
    
    @NotBlank(message = "Họ tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2-100 ký tự")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, message = "Mật khẩu phải ít nhất 8 ký tự")
    private String password;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Size(min = 10, max = 15, message = "Số điện thoại phải từ 10-15 ký tự")
    private String phone;

    private String avatar;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gender;

    @Past(message = "Ngày sinh phải là quá khứ")
    private Date birthday;

    @NotNull(message = "Trạng thái không được để trống")
    private EnumStatus status;

    // Optional fields for admin-specific information
    private String cccd; // Căn cước công dân
    
    private String department; // Phòng ban
    
    private String position; // Chức vụ
    
    private String notes; // Ghi chú
}
