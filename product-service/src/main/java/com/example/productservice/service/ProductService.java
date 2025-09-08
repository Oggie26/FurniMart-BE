package com.example.productservice.service;

import com.example.productservice.entity.Brand;
import com.example.productservice.entity.Category;
import com.example.productservice.entity.ImageProduct;
import com.example.productservice.entity.Product;
import com.example.productservice.enums.EnumStatus;
import com.example.productservice.enums.ErrorCode;
import com.example.productservice.exception.AppException;
import com.example.productservice.repository.BrandRepository;
import com.example.productservice.repository.CategoryRepository;
import com.example.productservice.repository.ImageProductRepository;
import com.example.productservice.repository.ProductRepository;
import com.example.productservice.request.ProductRequest;
import com.example.productservice.response.*;
import com.example.productservice.service.inteface.IProductService;
import com.example.productservice.util.SlugUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ImageProductRepository imageProductRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest) {
        Brand brand = brandRepository.findByIdAndIsDeletedFalse(productRequest.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        Category category = categoryRepository.findByIdAndIsDeletedFalse(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (productRepository.findByNameAndIsDeletedFalse(productRequest.getName()).isPresent()) {
            throw new AppException(ErrorCode.PRODUCT_NAME_EXISTED);
        }

        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .code(productRequest.getCode())
                .costPrice(productRequest.getCostPrice())
                .sellPrice(productRequest.getSellPrice())
                .unit(productRequest.getUnit())
                .quantityPerBox(productRequest.getQuantityPerBox())
                .status(productRequest.getStatus())
                .thumbnailImage(productRequest.getThumbnailImage())
                .unitPrice(productRequest.getUnitPrice())
                .slug(SlugUtil.toSlug(productRequest.getName()))
                .brand(brand)
                .category(category)
                .build();

        List<ImageProduct> images = null;
        if (productRequest.getImages() != null && !productRequest.getImages().isEmpty()) {
            images = productRequest.getImages().stream()
                    .map(req -> ImageProduct.builder()
                            .image(req.getImage())
                            .product(product)
                            .build())
                    .toList();
            product.setImageProduct(images);
        }
        assert images != null;
        imageProductRepository.saveAll(images);
        productRepository.save(product);
        return toProductResponse(product);
    }


    @Override
    @Transactional
    public ProductResponse updateProduct(ProductRequest productRequest, String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        Brand brand = brandRepository.findByIdAndIsDeletedFalse(productRequest.getBrandId())
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

        Category category = categoryRepository.findByIdAndIsDeletedFalse(productRequest.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        productRepository.findByNameAndIsDeletedFalse(productRequest.getName())
                .filter(existing -> !existing.getId().equals(productId))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.PRODUCT_NAME_EXISTED);
                });

        product.setName(productRequest.getName());
        product.setDescription(productRequest.getDescription());
        product.setCode(productRequest.getCode());
        product.setCostPrice(productRequest.getCostPrice());
        product.setSellPrice(productRequest.getSellPrice());
        product.setUnit(productRequest.getUnit());
        product.setQuantityPerBox(productRequest.getQuantityPerBox());
        product.setStatus(productRequest.getStatus());
        product.setThumbnailImage(productRequest.getThumbnailImage());
        product.setUnitPrice(productRequest.getUnitPrice());
        product.setSlug(SlugUtil.toSlug(productRequest.getName()));
        product.setBrand(brand);
        product.setCategory(category);

        productRepository.save(product);

        return toProductResponse(product);
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
        if(product.getStatus().equals(EnumStatus.ACTIVE)){
            product.setStatus(EnumStatus.INACTIVE);
        }else{
            product.setStatus(EnumStatus.ACTIVE);
        }
        productRepository.save(product);
    }

    @Override
    public ProductResponse getProduct(String productId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return toProductResponse(product);
    }

    @Override
    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll()
                .stream()
                .filter(product -> !product.getIsDeleted())
                .toList();

        List<ProductResponse> responses = new ArrayList<>();
        for (Product product : products) {
            responses.add(toProductResponse(product));
        }
        return responses;
    }

    @Override
    public PageResponse<ProductResponse> searchProduct(String request, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchByKeywordNative(request, pageable);

        List<ProductResponse> data = productPage.getContent().stream()
                .map(this::toProductResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                data,
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public ProductResponse toProductResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .code(product.getCode())
                .status(product.getStatus())
                .unit(product.getUnit())
                .costPrice(product.getCostPrice())
                .quantityPerBox(product.getQuantityPerBox())
                .sellPrice(product.getSellPrice())
                .thumbnailImage(product.getThumbnailImage())
                .unitPrice(product.getUnitPrice())
                .slug(product.getSlug())
                .images(product.getImageProduct().stream()
                        .map(img -> new ImageProductResponse(img.getId(), img.getImage()))
                        .toList())
                .categoryName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .build();
    }

}
