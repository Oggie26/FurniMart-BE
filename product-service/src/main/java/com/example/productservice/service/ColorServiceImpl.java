package com.example.productservice.service;

import com.example.productservice.entity.Color;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.ColorRepository;
import com.example.productservice.request.ColorRequest;
import com.example.productservice.response.ColorResponse;
import com.example.productservice.response.PageResponse;
import com.example.productservice.service.inteface.ColorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ColorServiceImpl implements ColorService {

    private final ColorRepository colorRepository;

    @Override
    @Transactional
    public ColorResponse addColor(ColorRequest colorRequest) {
        if(colorRepository.findByColorNameAndIsDeletedFalse(colorRequest.getColorName()).isPresent()){
            throw new AppException(ErrorCode.COLOR_NAME_EXISTED);
        }
        if (colorRepository.findByHexCodeAndIsDeletedFalse(colorRequest.getHexCode()).isPresent()) {
            throw new AppException(ErrorCode.HEX_CODE_EXISTED);
        }

        Color color = Color.builder()
                .colorName(colorRequest.getColorName())
                .hexCode(colorRequest.getHexCode())
                .build();

        colorRepository.save(color);
        return mapToResponse(color);
    }

    @Override
    @Transactional
    public ColorResponse updateColor(ColorRequest colorRequest, String colorId) {
        Color color = colorRepository.findByIdAndIsDeletedFalse(colorId)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

        colorRepository.findByHexCodeAndIsDeletedFalse(colorRequest.getHexCode())
                .filter(existing -> !existing.getId().equals(color.getId()))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.HEX_CODE_EXISTED);
                });

        colorRepository.findByColorNameAndIsDeletedFalse(colorRequest.getColorName())
                .filter(existing -> !existing.getId().equals(color.getId()))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.COLOR_NAME_EXISTED);
                });

        color.setColorName(colorRequest.getColorName());
        color.setHexCode(colorRequest.getHexCode());

        colorRepository.save(color);
        return mapToResponse(color);
    }

    @Override
    public ColorResponse getColorById(String colorId) {
        Color color = colorRepository.findByIdAndIsDeletedFalse(colorId)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));
        return mapToResponse(color);
    }

    @Override
    public List<ColorResponse> getAllColors() {
        return colorRepository.findAll()
                .stream()
                .filter(color -> !color.getIsDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteColor(String colorId) {
        Color color = colorRepository.findByIdAndIsDeletedFalse(colorId)
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));
        color.setIsDeleted(true);
        colorRepository.save(color);
    }

    @Override
    public PageResponse<ColorResponse> searchColors(String request, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Color> colorPage = colorRepository.searchByKeywordNative(request, pageable);

        List<ColorResponse> data = colorPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                colorPage.getNumber(),
                colorPage.getSize(),
                colorPage.getTotalElements(),
                colorPage.getTotalPages()
        );
    }

    private ColorResponse mapToResponse(Color color) {
        return ColorResponse.builder()
                .id(color.getId())
                .colorName(color.getColorName())
                .hexCode(color.getHexCode())
                .build();
    }
}
