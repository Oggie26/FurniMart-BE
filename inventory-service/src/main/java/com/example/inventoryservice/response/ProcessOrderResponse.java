package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumProcessOrder;
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
