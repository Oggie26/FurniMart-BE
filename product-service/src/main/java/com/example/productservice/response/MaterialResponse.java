package com.example.productservice.response;


import com.example.productservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MaterialResponse {
    private Long id;
    private String materialName;
    private String description;
    private EnumStatus status;
}
