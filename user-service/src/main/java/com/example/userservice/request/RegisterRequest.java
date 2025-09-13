package com.example.userservice.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Password must not be blank")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Phone must not be blank")
    @Size(min = 9, max = 15, message = "Phone must be between 9 and 15 digits")
    private String phone;

    @NotNull(message = "Gender must not be null")
    private Boolean gender;

    @NotBlank(message = "Full name must not be blank")
    private String fullName;

    @Past(message = "BirthDay must be a past date")
    private Date birthDay;


}
