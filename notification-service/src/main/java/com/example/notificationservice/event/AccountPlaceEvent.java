package com.example.notificationservice.event;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountPlaceEvent {
    private String id;
    private String fullName;
    private String email;
}
