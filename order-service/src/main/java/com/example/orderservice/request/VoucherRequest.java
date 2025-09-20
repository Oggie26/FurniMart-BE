package com.example.orderservice.request;

import com.example.orderservice.enums.VoucherType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDateTime startDate;

    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDateTime endDate;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Float amount;

    private String description;

    @NotNull(message = "Point is required")
    @PositiveOrZero(message = "Point must be positive or zero")
    private Integer point;

    @NotNull(message = "Type is required")
    private VoucherType type;

    @NotNull(message = "Status is required")
    private Boolean status;

    private Long orderId;

    @Positive(message = "Usage limit must be positive")
    private Integer usageLimit;

    @PositiveOrZero(message = "Minimum order amount must be positive or zero")
    private Float minimumOrderAmount;

    @AssertTrue(message = "End date must be after start date")
    public boolean isEndDateAfterStartDate() {
        if (startDate == null || endDate == null) {
            return true; // Let @NotNull handle null validation
        }
        return endDate.isAfter(startDate);
    }
}
