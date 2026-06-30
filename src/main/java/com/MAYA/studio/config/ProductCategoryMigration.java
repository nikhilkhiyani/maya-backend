package com.MAYA.studio.config;

import com.MAYA.studio.entity.Product;
import com.MAYA.studio.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Migrates legacy product categories (e.g. WOMEN) to the current category codes
 * used by the categories table (TUNICS, CO_ORDS, etc.).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryMigration implements ApplicationRunner {

    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Product> legacyProducts = productRepository.findAll().stream()
                .filter(p -> p.getCategory() != null && isLegacyCategory(p.getCategory()))
                .toList();

        if (legacyProducts.isEmpty()) {
            return;
        }

        for (Product product : legacyProducts) {
            product.setCategory(mapLegacyCategory(product));
            productRepository.save(product);
        }

        log.info("Migrated {} products from legacy categories to current category codes", legacyProducts.size());
    }

    private boolean isLegacyCategory(String category) {
        String normalized = category.toUpperCase();
        return normalized.equals("WOMEN")
                || normalized.equals("MEN")
                || normalized.equals("SAREE")
                || normalized.equals("LEHENGA")
                || normalized.equals("KURTI")
                || normalized.equals("JEWELRY")
                || normalized.equals("ACCESSORIES");
    }

    private String mapLegacyCategory(Product product) {
        String subcategory = product.getSubcategory() != null
                ? product.getSubcategory().toLowerCase()
                : "";

        if (subcategory.contains("coord") || subcategory.contains("co-ord")) {
            return "CO_ORDS";
        }
        if (subcategory.contains("dress")) {
            return "DRESSES";
        }
        if (subcategory.contains("top") || subcategory.contains("bottom")) {
            return "TOP_AND_BOTTOMS";
        }
        if (subcategory.contains("kurta") || subcategory.contains("tunic")) {
            return "TUNICS";
        }

        return "TUNICS";
    }
}
