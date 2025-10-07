package com.example.productservice.service;

import com.example.productservice.entity.Color;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.ColorRepository;
import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.service.inteface.ColorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final ColorRepository colorRepository;

    @Override
    @Transactional
    public ColorResponse addColor(ColorRequest colorRequest) {
        if (colorRepository.findByColorNameAndIsDeletedFalse(colorRequest.getColorName()).isPresent()) {
            throw new AppException(ErrorCode.COLOR_ALREADY_EXISTS);
        }

        Color color = Color.builder()
                .colorName(colorRequest.getColorName())
                .hexCode(colorRequest.getHexCode())
                .build();

        colorRepository.save(color);
        return mapToColorResponse(color);
    }

    @Override
    @Transactional
    public ColorResponse updateColor(ColorRequest colorRequest, String id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

        colorRepository.findByColorNameAndIsDeletedFalse(colorRequest.getColorName())
                .filter(existing -> !existing.getId().equals(color.getId()))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.COLOR_ALREADY_EXISTS);
                });

        color.setColorName(colorRequest.getColorName());
        color.setHexCode(colorRequest.getHexCode());

        colorRepository.save(color);
        return mapToColorResponse(color);
    }

    @Override
    @Transactional
    public void deleteColor(String id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

        // Xóa mềm
        color.setIsDeleted(true);
        colorRepository.save(color);
    }

    @Override
    public ColorResponse getColor(String id) {
        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

        return mapToColorResponse(color);
    }

    @Override
    public List<ColorResponse> getAllColors() {
        return colorRepository.findAll().stream()
                .filter(c -> !c.getIsDeleted())
                .map(this::mapToColorResponse)
                .toList();
    }

    private ColorResponse mapToColorResponse(Color color) {
        return ColorResponse.builder()
                .id(color.getId())
                .colorName(color.getColorName())
                .hexCode(color.getHexCode())
                .build();
    }
}
