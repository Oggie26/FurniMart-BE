package com.example.productservice.entity;

import com.example.productservice.enums.Enum3DFormat;
import com.example.productservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_model_3d")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductModel3D extends AbstractEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_color_id", nullable = false)
    private ProductColor productColor;

    private String modelUrl;

    @Enumerated(EnumType.STRING)
    private Enum3DFormat format;

    private Double sizeInMb;
    private String previewImage;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;
}
