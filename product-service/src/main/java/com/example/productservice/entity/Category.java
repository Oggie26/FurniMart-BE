package com.example.productservice.entity;

import com.example.productservice.enums.EnumStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "categories")
public class Category extends AbstractEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String categoryName;

    @Column
    private String description;

    @Column
    private String image;

    @Enumerated(EnumType.STRING)
    private EnumStatus  status;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL)
    private List<Product> products;


}

