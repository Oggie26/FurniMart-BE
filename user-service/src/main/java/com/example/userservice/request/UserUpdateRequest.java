package com.example.userservice.request;

import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {

    @Size(min = 1, message = "Full name cannot be empty")
    private String fullName;
    
    @Size(min = 1, message = "Phone cannot be empty")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Phone must be 10 digits starting with 0")
    private String phone;
    
    private String avatar;
    
    private Boolean gender;
    
    @Past(message = "Birthday must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date birthday;
    
    private EnumStatus status;
    
    private String cccd;
    
    private Integer point;
    
    // New fields for employee management
    private String storeId;
    
    @Enumerated(EnumType.STRING)
    private EnumRole role;

    // Manual getters and setters (Lombok not working in Docker build)
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Boolean getGender() { return gender; }
    public void setGender(Boolean gender) { this.gender = gender; }
    public Date getBirthday() { return birthday; }
    public void setBirthday(Date birthday) { this.birthday = birthday; }
    public EnumStatus getStatus() { return status; }
    public void setStatus(EnumStatus status) { this.status = status; }
    public String getCccd() { return cccd; }
    public void setCccd(String cccd) { this.cccd = cccd; }
    public Integer getPoint() { return point; }
    public void setPoint(Integer point) { this.point = point; }
    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }
    public EnumRole getRole() { return role; }
    public void setRole(EnumRole role) { this.role = role; }
}
