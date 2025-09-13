package com.example.productservice.entity;

import com.example.productservice.enums.EnumStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String materialName;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private EnumStatus status;

    @OneToMany(mappedBy = "material", cascade = CascadeType.ALL, orphanRemoval = false)
    @JsonIgnore
    private List<Product> products = new ArrayList<>();
}

