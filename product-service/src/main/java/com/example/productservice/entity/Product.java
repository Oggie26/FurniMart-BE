package com.example.productservice.entity;

import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.EnumUnit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(
        name = "products",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "code"),
                @UniqueConstraint(columnNames = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Double costPrice;

    @Column(nullable = false)
    private Double sellPrice;

    @Column(nullable = false)
    private Double unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatus status;

    @Column(nullable = false)
    private Integer quantityPerBox;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumUnit unit;

    @Column(nullable = false)
    private String thumbnailImage;

    @Column(nullable = false, unique = true)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;


}
