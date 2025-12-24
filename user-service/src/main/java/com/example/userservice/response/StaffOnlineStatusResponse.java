package com.example.userservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class StaffOnlineStatusResponse {
    private int onlineCount;
    private boolean hasOnlineStaff;
    private String estimatedWaitTime;
}

