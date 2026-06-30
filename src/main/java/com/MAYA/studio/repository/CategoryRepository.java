package com.MAYA.studio.repository;

import com.MAYA.studio.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByCode(String code);

    List<Category> findByEnabledTrueOrderByDisplayOrderAsc();

    List<Category> findByShowOnHomepageTrueAndEnabledTrueOrderByDisplayOrderAsc();

    boolean existsBySlug(String slug);

    boolean existsByCode(String code);
}
