package com.example.orderservice.response;

import com.example.orderservice.enums.VoucherType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {

    private Integer id;
    private String name;
    private String code;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Float amount;
    private String description;
    private Integer point;
    private VoucherType type;
    private Boolean status;
    private Long orderId;
    private Integer usageLimit;
    private Integer usedCount;
    private Float minimumOrderAmount;
    private Date createdAt;
    private Date updatedAt;
    private boolean isActive;
    private boolean isExpired;
}
