package com.example.productservice.service;

import com.example.productservice.entity.Material;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.MaterialRepository;
import com.example.productservice.request.MaterialRequest;
import com.example.productservice.response.MaterialResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.service.inteface.MaterialService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;

    @Override
    @Transactional
    public MaterialResponse createMaterial(MaterialRequest materialRequest) {
        if(materialRepository.findByMaterialNameAndIsDeletedFalse( materialRequest.getMaterialName()).isPresent()){
            throw new AppException(ErrorCode.MATERIAL_NAME_EXISTED);
        }

        Material material = Material.builder()
                .materialName(materialRequest.getMaterialName())
                .description(materialRequest.getDescription())
                .status(materialRequest.getStatus())
                .build();

        materialRepository.save(material);
        return mapToResponse(material);
    }

    @Override
    @Transactional
    public MaterialResponse updateMaterial(MaterialRequest materialRequest, Integer id) {
        Material material = materialRepository.findByIdAndIsDeletedFalse(id)
                        .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        materialRepository.findByMaterialNameAndIsDeletedFalse(materialRequest.getMaterialName())
                .filter(existing -> !existing.getId().equals(material.getId()))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.MATERIAL_EXISTED);
                });

        material.setDescription(materialRequest.getDescription());
        material.setMaterialName(materialRequest.getMaterialName());
        material.setStatus(materialRequest.getStatus());
        materialRepository.save(material);
        return mapToResponse(material);
    }

    @Override
    public void deleteMaterial(Integer id) {
        Material material = materialRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(()  -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));
        material.setStatus(EnumStatus.DELETED);
        material.setIsDeleted(true);
        materialRepository.save(material);
    }

    @Override
    public void disableMaterial(Integer id) {
        Material material = materialRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(()  -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));
        if(material.getStatus().equals(EnumStatus.ACTIVE)){
            material.setStatus(EnumStatus.ACTIVE);
        }else{
            material.setStatus(EnumStatus.INACTIVE);
        }
        materialRepository.save(material);
    }

    @Override
    public List<MaterialResponse> getAllMaterials() {
        return materialRepository.findAll()
                .stream()
                .filter(material -> !material.getIsDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public MaterialResponse getMaterialById(Integer id) {
        Material material = materialRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(()  -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));
        return mapToResponse(material);
    }

    @Override
    public PageResponse<MaterialResponse> searchMaterial(String request, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Material> materialPage = materialRepository.searchByKeywordNative(request, pageable);

        List<MaterialResponse> data = materialPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                materialPage.getNumber(),
                materialPage.getSize(),
                materialPage.getTotalElements(),
                materialPage.getTotalPages()
        );
    }

    private MaterialResponse mapToResponse(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .materialName(material.getMaterialName())
                .description(material.getDescription())
                .status(material.getStatus())
                .build();
    }
}
