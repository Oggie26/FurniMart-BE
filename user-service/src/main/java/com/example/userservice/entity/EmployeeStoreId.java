package com.example.userservice.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeStoreId implements Serializable {
    
    private String employeeId;
    private String storeId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EmployeeStoreId that = (EmployeeStoreId) o;
        return Objects.equals(employeeId, that.employeeId) && Objects.equals(storeId, that.storeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(employeeId, storeId);
    }
}

