package com.example.userservice.request;

import com.example.userservice.enums.EnumStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Builder
@Data
public class StaffUpdateRequest {
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String avatar;

    private Boolean gender;

    @Past(message = "Birthday must be in the past")
    private Date birthday;

    private String cccd;

    private String department;

    private String position;

    private BigDecimal salary;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    private List<String> storeIds;
}
