package com.example.productservice.service;

import com.example.productservice.entity.*;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.*;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.PageResponse;
import com.example.productservice.response.ProductResponse;
import com.example.productservice.service.inteface.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository  colorRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductModel3DRepository productModel3DRepository;


    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        if(productRepository.findByNameAndIsDeletedFalse(productRequest.getName()).isPresent()){
            throw new AppException(ErrorCode.PRODUCT_NAME_EXISTED);
        }
        if (productRepository.findByCodeAndIsDeletedFalse(productRequest.getCode()).isPresent()) {
            throw new AppException(ErrorCode.CODE_EXISTED);
        }

        Material material = materialRepository.findByIdAndIsDeletedFalse(productRequest.getMaterialId())
                .orElseThrow(() -> new AppException(ErrorCode.MATERIAL_NOT_FOUND));

        Category category = categoryRepository.findByIdAndIsDeletedFalse(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

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
                .material(material)
                .status(EnumStatus.ACTIVE)
                .build();

        List<ProductImage> imageList = new ArrayList<>();
        if (productRequest.getImageRequestList() != null && !productRequest.getImageRequestList().isEmpty()) {
            imageList = productRequest.getImageRequestList().stream()
                    .map(images -> {
                        ProductImage image = new ProductImage();
                        image.setImageUrl(images.getImageUrl());
                        image.setProduct(product);
                        return image;
                    })
                    .toList();
        }

        for (ProductImage image : imageList) {
            image.setProduct(product);
        }

        List<ProductModel3D> image3DList = new ArrayList<>();
        if (productRequest.getModel3DRequestList() != null && !productRequest.getModel3DRequestList().isEmpty()) {
            image3DList = productRequest.getModel3DRequestList().stream()
                    .map(req -> {
                        Color color = null;
                        if (req.getColorId() != null) {
                            color = colorRepository.findByIdAndIsDeletedFalse(req.getColorId())
                                    .orElseThrow(() -> new AppException(ErrorCode.COLOR_NOT_FOUND));
                        }
                        return ProductModel3D.builder()
                                .color(color)
                                .status(EnumStatus.ACTIVE)
                                .previewImage(req.getPreviewImage())
                                .format(req.getFormat())
                                .modelUrl(req.getModelUrl())
                                .sizeInMb(req.getSizeInMb())
                                .product(product)
                                .build();
                    })
                    .toList();
        }

        for (ProductModel3D image : image3DList) {
            image.setProduct(product);
        }


        List<Color> colorList = new ArrayList<>();
        if (productRequest.getColorRequests() != null && !productRequest.getColorRequests().isEmpty()) {
            colorList = productRequest.getColorRequests().stream()
                    .map(colors -> {
                        Color color = new Color();
                        color.setColorName(colors.getColorName());
                        color.setHexCode(colors.getHexCode());
                        color.setProduct(product);
                        return color;
                    })
                    .toList();
        }

        for (Color color : colorList) {
            color.setProduct(product);
        }

        productImageRepository.saveAll(imageList);
        productModel3DRepository.saveAll(image3DList);
        colorRepository.saveAll(colorList);
        productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(ProductRequest productRequest, String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        productRepository.save(product);
        return mapToResponse(product);
    }

    @Override
    public void deleteProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        product.setIsDeleted(true);
        product.setStatus(EnumStatus.DELETED);
        productRepository.save(product);
    }

    @Override
    public void disableProduct(String productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if(product.getStatus().equals(EnumStatus.ACTIVE)){
            product.setStatus(EnumStatus.INACTIVE);
        }else{
            product.setStatus(EnumStatus.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findById(productId)
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
    public PageResponse<ProductResponse> searchProduct(String request, int page, int size) {
        return null;
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .slug(product.getSlug())
                .code(product.getCode())
                .category(product.getCategory())
                .thumbnailImage(product.getThumbnailImage())
                .width(product.getWidth())
                .height(product.getHeight())
                .length(product.getLength())
                .weight(product.getWeight())
                .status(product.getStatus())
                .build();
    }
}
