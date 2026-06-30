package com.MAYA.studio.service;

import com.MAYA.studio.dto.ProductRequest;
import com.MAYA.studio.dto.ProductResponse;
import com.MAYA.studio.entity.Product;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.ProductMapper;
import com.MAYA.studio.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    private final com.MAYA.studio.service.FileStorageService fileStorageService;


    public Page<ProductResponse> getAllProducts(String category, String search, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        Specification<Product> spec = (root, query, cb) -> cb.conjunction();
        
        if (category != null && !category.isEmpty()) {
            String normalized = category.toUpperCase().replace("-", "_");
            spec = spec.and((root, query, cb) ->
                cb.equal(cb.upper(root.get("category")), normalized));
        }
        
        if (search != null && !search.isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("description")), "%" + search.toLowerCase() + "%")
                ));
        }
        
        if (minPrice != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        
        if (maxPrice != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        
        return productRepository.findAll(spec, pageable)
                .map(productMapper::toResponse);
    }
    
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return productMapper.toResponse(product);
    }
    
//    @Transactional
//    public ProductResponse createProduct(ProductRequest request) {
//        Product product = productMapper.toEntity(request);
//        Product savedProduct = productRepository.save(product);
//        return productMapper.toResponse(savedProduct);
//    }


    @Transactional
    public ProductResponse createProductJson(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        if (request.getCategory() != null) {
            product.setCategory(normalizeCategoryCode(request.getCategory()));
        }
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            product.setImages(request.getImages());
        }
        if (product.getSlug() == null || product.getSlug().isBlank()) {
            product.setSlug(product.getName().toLowerCase().replaceAll("\\s+", "-"));
        }
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProductJson(UUID id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        if (request.getCategory() != null) {
            product.setCategory(normalizeCategoryCode(request.getCategory()));
        }
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setStock(request.getStock());
        if (request.getImages() != null) {
            product.setImages(request.getImages());
        }
        product.setIsReadyToShip(request.getIsReadyToShip());
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            product.setSlug(request.getSlug());
        }
        product.setSubcategory(request.getSubcategory());
        product.setFabric(request.getFabric());
        if (request.getIsFeatured() != null) {
            product.setIsFeatured(request.getIsFeatured());
        }
        if (request.getRating() != null) {
            product.setRating(request.getRating());
        }
        if (request.getReviews() != null) {
            product.setReviews(request.getReviews());
        }

        return productMapper.toResponse(productRepository.save(product));
    }

    private String normalizeCategoryCode(String category) {
        return category.toUpperCase().replace("-", "_");
    }

    @Transactional
    public ProductResponse createProduct(
            ProductRequest request,
            MultipartFile image
    ) {

//        String imageUrl = fileStorageService.uploadFile(image);
        String imageUrl = fileStorageService.uploadFile(image, "products");
        Product product = productMapper.toEntity(request);
        product.setImages(List.of(imageUrl));
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(
            UUID id,
            ProductRequest request,
            MultipartFile image
    ) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with id: " + id
                        ));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(request.getCategory());

        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());

        product.setStock(request.getStock());

        product.setIsReadyToShip(
                request.getIsReadyToShip()
        );

        // NEW FIELDS
        product.setSlug(
                request.getSlug()
        );

        product.setSubcategory(
                request.getSubcategory()
        );

        product.setFabric(
                request.getFabric()
        );

        product.setIsFeatured(
                request.getIsFeatured()
        );

        product.setReviews(
                request.getReviews()
        );

        // upload new image only if provided
        if (image != null && !image.isEmpty()) {

            String imageUrl =
                    fileStorageService.uploadFile(image, "products");

            product.setImages(
                    List.of(imageUrl)
            );
        }

        Product updatedProduct =
                productRepository.save(product);

        return productMapper.toResponse(
                updatedProduct
        );
    }
    
    @Transactional
    public void deleteProduct(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }

    public ProductResponse getBySlug(String slug) {
        Product product = productRepository
                .findBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Product not found with slug: " + slug
                        ));

        return productMapper.toResponse(product);
    }
}
