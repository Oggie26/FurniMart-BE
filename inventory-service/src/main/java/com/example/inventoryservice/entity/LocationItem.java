package com.example.inventoryservice.entity;
import com.example.inventoryservice.enums.EnumRowLabel;
import com.example.inventoryservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "location_item")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationItem extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumRowLabel rowLabel;

    @Column(nullable = false)
    private Integer columnNumber;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JsonIgnore
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    @OneToMany(mappedBy = "locationItem")
    @JsonIgnore
    private List<Inventory> inventories;

    @PrePersist
    public void generateCode() {
        if (zone != null && zone.getZoneCode() != null) {
            this.code = zone.getZoneCode().name() + "-" + rowLabel.name() + "-C" + columnNumber;
        }
    }
}

