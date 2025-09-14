package com.example.userservice.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserStoreId implements Serializable {
    
    private String userId;
    private String storeId;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserStoreId that = (UserStoreId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(storeId, that.storeId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(userId, storeId);
    }
}
