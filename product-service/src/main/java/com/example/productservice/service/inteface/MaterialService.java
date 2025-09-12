package com.example.productservice.service.inteface;

import com.example.productservice.request.MaterialRequest;
import com.example.productservice.response.MaterialResponse;
import com.example.productservice.response.PageResponse;

import java.util.List;

public interface MaterialService {

    MaterialResponse createMaterial(MaterialRequest materialRequest);
    MaterialResponse updateMaterial(MaterialRequest materialRequest, Integer id);
    void deleteMaterial(Integer id);
    void disableMaterial(Integer id);
    List<MaterialResponse> getAllMaterials();
    MaterialResponse getMaterialById(Integer id);
    PageResponse<MaterialResponse> searchMaterial(String request, int page, int size);

}
