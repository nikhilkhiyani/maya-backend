package com.MAYA.studio.service;

import com.MAYA.studio.dto.CategoryRequest;
import com.MAYA.studio.dto.CategoryResponse;
import com.MAYA.studio.entity.Category;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.CategoryRepository;
import com.MAYA.studio.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryResponse> getAll(boolean admin) {
        List<Category> categories = admin
                ? categoryRepository.findAll()
                : categoryRepository.findByEnabledTrueOrderByDisplayOrderAsc();
        return categories.stream()
                .sorted((a, b) -> Integer.compare(
                        a.getDisplayOrder() != null ? a.getDisplayOrder() : 0,
                        b.getDisplayOrder() != null ? b.getDisplayOrder() : 0))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<CategoryResponse> getHomepageCategories() {
        return categoryRepository.findByShowOnHomepageTrueAndEnabledTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public CategoryResponse getBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = normalizeSlug(request.getSlug() != null ? request.getSlug() : request.getName());
        String code = normalizeCode(request.getCode() != null ? request.getCode() : slug);

        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category slug already exists");
        }
        if (categoryRepository.existsByCode(code)) {
            throw new BadRequestException("Category code already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .code(code)
                .description(request.getDescription())
                .image(request.getImage())
                .bannerImage(request.getBannerImage())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .showOnHomepage(request.getShowOnHomepage() != null ? request.getShowOnHomepage() : true)
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (request.getName() != null) category.setName(request.getName());
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String slug = normalizeSlug(request.getSlug());
            if (!slug.equals(category.getSlug()) && categoryRepository.existsBySlug(slug)) {
                throw new BadRequestException("Category slug already exists");
            }
            category.setSlug(slug);
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            String code = normalizeCode(request.getCode());
            if (!code.equals(category.getCode()) && categoryRepository.existsByCode(code)) {
                throw new BadRequestException("Category code already exists");
            }
            category.setCode(code);
        }
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImage() != null) category.setImage(request.getImage());
        if (request.getBannerImage() != null) category.setBannerImage(request.getBannerImage());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getEnabled() != null) category.setEnabled(request.getEnabled());
        if (request.getFeatured() != null) category.setFeatured(request.getFeatured());
        if (request.getShowOnHomepage() != null) category.setShowOnHomepage(request.getShowOnHomepage());
        if (request.getSeoTitle() != null) category.setSeoTitle(request.getSeoTitle());
        if (request.getSeoDescription() != null) category.setSeoDescription(request.getSeoDescription());

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        long count = productRepository.countByCategory(category.getCode());
        if (count > 0) {
            throw new BadRequestException("Cannot delete category with " + count + " products");
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .code(category.getCode())
                .description(category.getDescription())
                .image(category.getImage())
                .bannerImage(category.getBannerImage())
                .displayOrder(category.getDisplayOrder())
                .enabled(category.getEnabled())
                .featured(category.getFeatured())
                .showOnHomepage(category.getShowOnHomepage())
                .seoTitle(category.getSeoTitle())
                .seoDescription(category.getSeoDescription())
                .createdAt(category.getCreatedAt())
                .productCount(productRepository.countByCategory(category.getCode()))
                .build();
    }

    private String normalizeSlug(String value) {
        return value.toLowerCase().trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }

    private String normalizeCode(String value) {
        return value.toUpperCase().trim().replace("-", "_");
    }
}
