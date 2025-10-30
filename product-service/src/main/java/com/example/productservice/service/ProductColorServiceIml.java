package com.example.productservice.service;

import com.example.productservice.entity.*;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.event.ProductCreatedEvent;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.*;
import com.example.productservice.request.ProductColorRequest;
import com.example.productservice.response.*;
import com.example.productservice.service.inteface.ProductColorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductColorServiceIml implements ProductColorService {
    private final ProductColorRepository productColorRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final ProductModel3DRepository productModel3DRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public ProductColorResponse addProductColor(ProductColorRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if(productColorRepository.existsByProductIdAndColorId(request.getProductId(), request.getColorId())) {
            throw new AppException(ErrorCode.COLOR_ALREADY_EXISTS);
        }

        Color color = colorRepository.findById(request.getColorId())
                .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

        ProductColor productColor = ProductColor.builder()
                .product(product)
                .color(color)
                .status(request.getStatus() != null ? request.getStatus() : EnumStatus.ACTIVE)
                .build();

        productColorRepository.save(productColor);

        if (request.getImageRequests() != null && !request.getImageRequests().isEmpty()) {
            List<ProductImage> images = request.getImageRequests().stream()
                    .map(imgReq -> ProductImage.builder()
                            .productColor(productColor)
                            .imageUrl(imgReq.getImageUrl())
                            .build())
                    .toList();
            productImageRepository.saveAll(images);
            productColor.setImages(images);
        }

        if (request.getModel3DRequests() != null && !request.getModel3DRequests().isEmpty()) {
            List<ProductModel3D> models3D = request.getModel3DRequests().stream()
                    .map(modelReq -> ProductModel3D.builder()
                            .productColor(productColor)
                            .status(modelReq.getStatus())
                            .modelUrl(modelReq.getModelUrl())
                            .format(modelReq.getFormat())
                            .sizeInMb(modelReq.getSizeInMb())
                            .previewImage(modelReq.getPreviewImage())
                            .build())
                    .toList();
            productModel3DRepository.saveAll(models3D);
            productColor.setModels3D(models3D);
        }

        return mapToResponse(productColor);
    }

    @Override
    @Transactional
    public ProductColorResponse updateProductColor(ProductColorRequest request, String id) {
        ProductColor existing = productColorRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            existing.setProduct(product);
        }

        if (request.getColorId() != null) {
            Color color = colorRepository.findById(request.getColorId())
                    .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));

            boolean colorExists = productColorRepository.existsByProductIdAndColorIdAndIdNot(
                    existing.getProduct().getId(),
                    request.getColorId(),
                    existing.getId()
            );
            if (colorExists) {
                throw new AppException(ErrorCode.COLOR_ALREADY_EXISTS);
            }

            existing.setColor(color);
        }

        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }

        if (request.getImageRequests() != null) {
            existing.getImages().clear();
            List<ProductImage> images = request.getImageRequests().stream()
                    .map(imgReq -> ProductImage.builder()
                            .productColor(existing)
                            .imageUrl(imgReq.getImageUrl())
                            .build())
                    .toList();
            existing.getImages().addAll(images);
        }

        if (request.getModel3DRequests() != null) {
            existing.getModels3D().clear();
            List<ProductModel3D> models3D = request.getModel3DRequests().stream()
                    .map(modelReq -> ProductModel3D.builder()
                            .productColor(existing)
                            .status(modelReq.getStatus())
                            .modelUrl(modelReq.getModelUrl())
                            .format(modelReq.getFormat())
                            .sizeInMb(modelReq.getSizeInMb())
                            .previewImage(modelReq.getPreviewImage())
                            .build())
                    .toList();
            existing.getModels3D().addAll(models3D);
        }

        ProductColor saved = productColorRepository.save(existing);
        return mapToResponse(saved);
    }

    @Override
    public void deleteProductColor(String productColorId) {
        ProductColor productColor = productColorRepository.findById(productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));
        productColorRepository.delete(productColor);
    }

    @Override
    public void disableProductColor(String productColorId) {
        ProductColor productColor = productColorRepository.findById(productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));
        if (productColor.getStatus() == EnumStatus.ACTIVE) {
            productColor.setStatus(EnumStatus.INACTIVE);
        }else{
        productColor.setStatus(EnumStatus.INACTIVE);
        }
        productColorRepository.save(productColor);
    }

    @Override
    public List<ProductColorResponse> getProductColors() {
        return productColorRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductColorResponse getProductColor(String productColorId) {
        ProductColor productColor = productColorRepository.findById(productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_COLOR_NOT_FOUND));
        return mapToResponse(productColor);
    }

    private ProductColorResponse mapToResponse(ProductColor entity) {
        return ProductColorResponse.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .product(mapProductToResponse(entity.getProduct()))
                .color(mapColorToResponse(entity.getColor()))
                .images(entity.getImages() != null
                        ? entity.getImages().stream()
                        .map(img -> ImageResponse.builder()
                                .id(img.getId())
                                .image(img.getImageUrl())
                                .build())
                        .toList()
                        : List.of())
                .models3D(entity.getModels3D() != null
                        ? entity.getModels3D().stream()
                        .map(m -> Image3DResponse.builder()
                                .image3d(m.getId())
                                .status(m.getStatus())
                                .modelUrl(m.getModelUrl())
                                .format(m.getFormat())
                                .sizeInMb(m.getSizeInMb())
                                .previewImage(m.getPreviewImage())
                                .build())
                        .toList()
                        : List.of())
                .build();
    }

    private ProductResponse mapProductToResponse(Product product) {
        if (product == null) return null;
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .code(product.getCode())
                .description(product.getDescription())
                .price(product.getPrice())
                .width(product.getWidth())
                .code(product.getCode())
                .description(product.getDescription())
                .height(product.getHeight())
                .weight(product.getWeight())
                .length(product.getLength())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getCategoryName())
                .materials(product.getMaterials() != null ? product.getMaterials().stream()
                        .map(m -> MaterialResponse.builder()
                                .id(m.getId())
                                .materialName(m.getMaterialName())
                                .description(m.getDescription())
                                .status(m.getStatus())
                                .image(m.getImage())
                                .build())
                        .toList() : null)
                .thumbnailImage(product.getThumbnailImage())
                .build();
    }

    private ColorResponse mapColorToResponse(Color color) {
        if (color == null) return null;
        return ColorResponse.builder()
                .id(color.getId())
                .colorName(color.getColorName())
                .hexCode(color.getHexCode())
                .build();
    }


}
