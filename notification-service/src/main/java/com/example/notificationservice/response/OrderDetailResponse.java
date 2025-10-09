
package com.example.notificationservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderDetailResponse {

    private Long id;
    private String productColorId;
    private Integer quantity;
    private Double price;
}
