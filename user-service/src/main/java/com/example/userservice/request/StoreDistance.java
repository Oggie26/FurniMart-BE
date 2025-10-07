package com.example.userservice.request;

import com.example.userservice.entity.Store;
import com.example.userservice.response.StoreResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDistance {
    private StoreResponse store;
    private Double distance;

}
