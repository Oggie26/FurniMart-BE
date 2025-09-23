package com.example.productservice.service;

import com.example.productservice.entity.*;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.*;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.*;
import com.example.productservice.service.inteface.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository colorRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductModel3DRepository productModel3DRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        if (productRepository.findByNameAndIsDeletedFalse(productRequest.getName()).isPresent()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_EXISTED);
        }
        if (productRepository.findByCodeAndIsDeletedFalse(productRequest.getCode()).isPresent()) {
            throw new AppException(ErrorCode.CODE_EXISTED);
        }

        Category category = categoryRepository.findByIdAndIsDeletedFalse(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<Material> materials = materialRepository.findAllById(productRequest.getMaterialIds());
        if (materials.isEmpty()) {
            throw new AppException(ErrorCode.MATERIAL_NOT_FOUND);
        }

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .code(productRequest.getCode())
                .price(productRequest.getPrice())
                .category(category)
                .weight(productRequest.getWeight())
                .width(productRequest.getWidth())
                .height(productRequest.getHeight())
                .length(productRequest.getLength())
                .thumbnailImage(productRequest.getThumbnailImage())
                .status(EnumStatus.ACTIVE)
                .materials(materials)
                .build();
        productRepository.save(product);

        if (productRequest.getColorRequests() != null) {
            List<Color> colors = productRequest.getColorRequests().stream()
                    .map(colorReq -> {
                        Color color = Color.builder()
                                .colorName(colorReq.getColorName())
                                .hexCode(colorReq.getHexCode())
                                .product(product)
                                .build();
                        colorRepository.save(color);

                        if (colorReq.getImageRequestList() != null) {
                            List<ProductImage> images = colorReq.getImageRequestList().stream()
                                    .map(imgReq -> ProductImage.builder()
                                            .imageUrl(imgReq.getImageUrl())
                                            .color(color)
                                            .build())
                                    .toList();
                            productImageRepository.saveAll(images);
                            color.setImages(images);
                        }

                        if (colorReq.getModel3DRequestList() != null) {
                            List<ProductModel3D> models = colorReq.getModel3DRequestList().stream()
                                    .map(modelReq -> ProductModel3D.builder()
                                            .status(EnumStatus.ACTIVE)
                                            .modelUrl(modelReq.getModelUrl())
                                            .previewImage(modelReq.getPreviewImage())
                                            .format(modelReq.getFormat())
                                            .sizeInMb(modelReq.getSizeInMb())
                                            .color(color)
                                            .build())
                                    .toList();
                            productModel3DRepository.saveAll(models);
                            color.setModels3D(models);
                        }
                        return color;
                    })
                    .toList();
            product.setColors(colors);
        }

        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(ProductRequest productRequest, String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        productRepository.findByNameAndIsDeletedFalse(productRequest.getName())
                .filter(p -> !p.getId().equals(productId))
                .ifPresent(p -> { throw new AppException(ErrorCode.PRODUCT_NAME_EXISTED); });
        productRepository.findByCodeAndIsDeletedFalse(productRequest.getCode())
                .filter(p -> !p.getId().equals(productId))
                .ifPresent(p -> { throw new AppException(ErrorCode.CODE_EXISTED); });

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setCode(productRequest.getCode());
        product.setPrice(productRequest.getPrice());
        product.setWeight(productRequest.getWeight());
        product.setWidth(productRequest.getWidth());
        product.setHeight(productRequest.getHeight());
        product.setLength(productRequest.getLength());
        product.setThumbnailImage(productRequest.getThumbnailImage());

        Category category = categoryRepository.findByIdAndIsDeletedFalse(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        product.setCategory(category);

        List<Material> materials = materialRepository.findAllById(productRequest.getMaterialIds());
        if (materials.isEmpty()) {
            throw new AppException(ErrorCode.MATERIAL_NOT_FOUND);
        }
        product.setMaterials(materials);

        colorRepository.deleteAllByProductId(productId);

        if (productRequest.getColorRequests() != null) {
            List<Color> colors = productRequest.getColorRequests().stream()
                    .map(colorReq -> {
                        Color color = Color.builder()
                                .colorName(colorReq.getColorName())
                                .hexCode(colorReq.getHexCode())
                                .product(product)
                                .build();
                        colorRepository.save(color);

                        if (colorReq.getImageRequestList() != null) {
                            List<ProductImage> images = colorReq.getImageRequestList().stream()
                                    .map(imgReq -> ProductImage.builder()
                                            .imageUrl(imgReq.getImageUrl())
                                            .color(color)
                                            .build())
                                    .toList();
                            productImageRepository.saveAll(images);
                            color.setImages(images);
                        }

                        if (colorReq.getModel3DRequestList() != null) {
                            List<ProductModel3D> models = colorReq.getModel3DRequestList().stream()
                                    .map(modelReq -> ProductModel3D.builder()
                                            .status(EnumStatus.ACTIVE)
                                            .modelUrl(modelReq.getModelUrl())
                                            .previewImage(modelReq.getPreviewImage())
                                            .format(modelReq.getFormat())
                                            .sizeInMb(modelReq.getSizeInMb())
                                            .color(color)
                                            .build())
                                    .toList();
                            productModel3DRepository.saveAll(models);
                            color.setModels3D(models);
                        }
                        return color;
                    })
                    .toList();
            product.setColors(colors);
        }

        productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsDeleted(true);
        product.setStatus(EnumStatus.DELETED);
        productRepository.save(product);
    }

    @Override
    public void disableProduct(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setStatus(product.getStatus().equals(EnumStatus.ACTIVE)
                ? EnumStatus.INACTIVE : EnumStatus.ACTIVE);
        productRepository.save(product);
    }

    @Override
    public ProductResponse getProductById(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    @Override
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return mapToResponse(product);
    }

    @Override
    public List<ProductResponse> getProducts() {
        return productRepository.findAll()
                .stream()
                .filter(product -> !product.getIsDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategoryId(Long categoryId) {
        List<Product> products = productRepository.findByCategoryIdAndIsDeletedFalse(categoryId);
        return products.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PageResponse<ProductResponse> searchProduct(String request, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        Page<Product> producPage = productRepository.searchByKeywordNative(request, pageable);

        List<ProductResponse> data = producPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                producPage.getNumber(),
                producPage.getSize(),
                producPage.getTotalElements(),
                producPage.getTotalPages()
        );
    }

    @Override
    public ProductResponse getProductByColorId(String colorId, String productId) {
        Product product = productRepository.findProductByIdAndColorId(productId,colorId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return mapToResponse(product);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .slug(product.getSlug())
                .code(product.getCode())
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .thumbnailImage(product.getThumbnailImage())
                .width(product.getWidth())
                .height(product.getHeight())
                .length(product.getLength())
                .weight(product.getWeight())
                .status(product.getStatus())
                .materials(product.getMaterials() != null ? product.getMaterials().stream()
                        .map(m -> MaterialResponse.builder()
                                .id(m.getId())
                                .materialName(m.getMaterialName())
                                .description(m.getDescription())
                                .status(m.getStatus())
                                .image(m.getImage())
                                .build())
                        .toList() : null)
                .color(product.getColors() != null ? product.getColors().stream()
                        .map(c -> ColorResponse.builder()
                                .id(c.getId())
                                .colorName(c.getColorName())
                                .hexCode(c.getHexCode())
                                .images(c.getImages() != null ? c.getImages().stream()
                                        .map(i -> new ImageResponse(i.getImageUrl()))
                                        .toList() : null)
                                .models3D(c.getModels3D() != null ? c.getModels3D().stream()
                                        .map(m -> new Image3DResponse(
                                                m.getModelUrl(),
                                                m.getStatus(),
                                                m.getModelUrl(),
                                                m.getFormat(),
                                                m.getSizeInMb(),
                                                m.getPreviewImage()
                                        ))
                                        .toList() : null)
                                .build())
                        .toList() : null)
                .build();
    }
}
