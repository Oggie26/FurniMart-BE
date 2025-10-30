package com.example.productservice.entity;

import com.example.productservice.enums.EnumStatus;
import com.example.productservice.util.SlugUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
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

    @Column(name = "sell_price", nullable = false)
    private Double price;

    @Column(nullable = false)
    private String thumbnailImage;

    @Column
    private Double weight;

    @Column
    private Double height;

    @Column
    private Double width;

    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    @Column
    private String slug;

    @Column
    private Double length;

//    @Column(nullable = false)
//    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnore
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_materials",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "material_id")
    )
    private List<Material> materials = new ArrayList<>();


    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductColor> productColors = new ArrayList<>();


    @PrePersist
    @PreUpdate
    public void generateSlug() {
        if (this.category != null && this.name != null) {
            this.slug = SlugUtil.toSlug(this.category.getCategoryName()) + "/" + SlugUtil.toSlug(this.name);
        }
    }
}
