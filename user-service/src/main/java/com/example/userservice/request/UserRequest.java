package com.example.userservice.request;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
public class UserRequest {
    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    private String storeId;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    private String avatar;

    @NotNull(message = "Giới tính không được để trống")
    private Boolean gender;

    @Past(message = "Ngày sinh phải là quá khứ")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;

    @NotNull(message = "Vai trò không được để trống")
    @Enumerated(EnumType.STRING)
    private EnumRole role;

    @NotNull(message = "Trạng thái không được để trống")
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
}
