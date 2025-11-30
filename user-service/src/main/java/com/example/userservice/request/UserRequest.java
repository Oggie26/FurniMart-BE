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

    // Manual getters and setters (Lombok not working in Docker build)
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }
    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }
    public EnumRole getRole() { return role; }
    public void setRole(EnumRole role) { this.role = role; }
    public EnumStatus getStatus() { return status; }
    public void setStatus(EnumStatus status) { this.status = status; }
}
