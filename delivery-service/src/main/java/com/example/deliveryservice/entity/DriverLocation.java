package com.example.deliveryservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_locations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverLocation {

    @Id
    private Long id;

    @Column
    private String driverId;

    @Column
    private Long orderId;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    private LocalDateTime updatedAt;
}

