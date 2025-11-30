package com.example.inventoryservice.entity;

import com.example.inventoryservice.enums.EnumZone;
import com.example.inventoryservice.enums.ZoneStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "zone")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Zone extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String zoneName;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ZoneStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumZone zoneCode;

    @Column(nullable = false)
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @OneToMany(mappedBy = "zone", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<LocationItem> locationItems;
}
