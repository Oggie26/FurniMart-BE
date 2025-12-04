package com.example.productservice.service;

import com.example.productservice.entity.*;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.event.ProductCreatedEvent;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.*;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.*;
import com.example.productservice.service.inteface.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

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
                .status(productRequest.getStatus())
                .weight(productRequest.getWeight())
                .width(productRequest.getWidth())
                .height(productRequest.getHeight())
                .length(productRequest.getLength())
                .thumbnailImage(productRequest.getThumbnailImage())
                .materials(materials)
                .build();
        productRepository.save(product);

//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
//            @Override
//            public void afterCommit() {
//                try {
//                    ProductCreatedEvent event = ProductCreatedEvent.builder()
//                            .productId(product.getId())
//                            .name(product.getName())
//                            .price(product.getPrice())
//                            .categoryName(product.getCategory().getCategoryName())
//                            .description(productRequest.getDescription())
//                            .build();
//
//                    kafkaTemplate.send("product-create-topic", event);
//                    log.info("🧠 Sent AI event for product [{}]", product.getName());
//                } catch (Exception e) {
//                    log.error("Failed to send AI event for product [{}]: {}", product.getName(), e.getMessage());
//                }
//            }
//        });

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
        product.setDescription(productRequest.getDescription());
        product.setStatus(productRequest.getStatus());
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
        productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsDeleted(true);
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
        // Sanitize search keyword to prevent injection
        if (request != null && !request.trim().isEmpty()) {
            String trimmed = request.trim();
            trimmed = trimmed.replaceAll("[<>\"'%;()&+]", "");
            request = trimmed.length() > 100 ? trimmed.substring(0, 100) : trimmed;
        } else {
            request = "";
        }
        
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


    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .code(product.getCode())
                .slug(product.getSlug())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .thumbnailImage(product.getThumbnailImage())
                .width(product.getWidth())
                .height(product.getHeight())
//                .fullName()
//                .userId()
                .productColors(product.getProductColors() != null ?
                        product.getProductColors().stream()
                                .map(this::mapProductColorToDTO)
                                .toList() : null)
                .status(product.getStatus())
                .length(product.getLength())
                .weight(product.getWeight())
                .materials(product.getMaterials() != null ? product.getMaterials().stream()
                        .map(m -> MaterialResponse.builder()
                                .id(m.getId())
                                .materialName(m.getMaterialName())
                                .description(m.getDescription())
                                .status(m.getStatus())
                                .image(m.getImage())
                                .build())
                        .toList() : null)
                .build();
    }

    private ProductColorDTO mapProductColorToDTO(ProductColor productColor) {
        return ProductColorDTO.builder()
                .id(productColor.getId())
                .color(ColorResponse.builder()
                        .id(productColor.getColor().getId())
                        .colorName(productColor.getColor().getColorName())
                        .hexCode(productColor.getColor().getHexCode())
                        .build())
                .images(productColor.getImages() != null ?
                        productColor.getImages().stream()
                                .map(img -> ImageResponse.builder()
                                        .id(img.getId())
                                        .image(img.getImageUrl())
                                        .build())
                                .toList() : null)
                .models3D(productColor.getModels3D() != null ?
                        productColor.getModels3D().stream()
                                .map(model -> Image3DResponse.builder()
                                        .image3d(model.getId())
                                        .modelUrl(model.getModelUrl())
                                        .format(model.getFormat())
                                        .previewImage(model.getPreviewImage())
                                        .sizeInMb(model.getSizeInMb())
                                        .status(model.getStatus())
                                        .build())
                                .toList() : null)
                .status(productColor.getStatus())
                .build();
    }

//    private String getUserId() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()
//                || "anonymousUser".equals(authentication.getPrincipal())) {
//            throw new AppException(ErrorCode.UNAUTHENTICATED);
//        }
//
//        String username = authentication.getName();
//        ApiResponse<AuthResponse> response = authClient.getUserByUsername(username);
//
//        if (response == null || response.getData() == null) {
//            throw new AppException(ErrorCode.NOT_FOUND_USER);
//        }
//
//        ApiResponse<UserResponse> userId = userClient.getUserByAccountId(response.getData().getId());
//        if (userId == null || userId.getData() == null) {
//            throw new AppException(ErrorCode.NOT_FOUND_USER);
//        }
//
//        return userId.getData().getId();
//    }
}
