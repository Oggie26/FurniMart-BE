package com.example.userservice.request;

import com.example.userservice.enums.EnumStatus;
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
public class StaffRequest {
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Password is required")
    private String password;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String avatar;

    @NotNull(message = "Gender is required")
    private Boolean gender;

    @Past(message = "Birthday must be in the past")
    private Date birthday;

    private String cccd;

    private String department;

    private String position;

    private Double salary;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private EnumStatus status;
}
