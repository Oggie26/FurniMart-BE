package com.example.productservice.entity;

import com.example.productservice.enums.Enum3DFormat;
import com.example.productservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_models_3d")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductModel3D {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnumStatus status;

    @Column(nullable = false)
    private String modelUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Enum3DFormat format;

    @Column(nullable = false)
    private Double sizeInMb;

    @Column(nullable = false)
    private String previewImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnore
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_id")
    private Color color;
}


