package com.example.orderservice.response;

import com.example.orderservice.enums.EnumProcessOrder;
import lombok.*;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProcessOrderResponse {

    private Long id;
    private EnumProcessOrder status;
    private Date createdAt;
}
