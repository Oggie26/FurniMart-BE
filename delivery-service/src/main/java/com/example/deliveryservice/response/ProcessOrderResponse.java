package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessOrderResponse {
    private Long id;
    private EnumProcessOrder status;
    private Date createdAt;
}

