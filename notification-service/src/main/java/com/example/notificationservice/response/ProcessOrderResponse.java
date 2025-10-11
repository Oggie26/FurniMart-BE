package com.example.notificationservice.response;

import com.example.notificationservice.enums.EnumProcessOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
