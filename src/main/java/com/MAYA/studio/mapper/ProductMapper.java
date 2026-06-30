package com.MAYA.studio.mapper;

import com.MAYA.studio.dto.ProductRequest;
import com.MAYA.studio.dto.ProductResponse;
import com.MAYA.studio.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);
}
