package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDistanceResponse {
    private StoreResponse store;
    private Double distance; // in kilometers
}

