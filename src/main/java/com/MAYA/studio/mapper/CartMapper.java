package com.MAYA.studio.mapper;

import com.MAYA.studio.dto.CartResponse;
import com.MAYA.studio.entity.Cart;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface CartMapper {
    
    CartResponse toResponse(Cart cart);
}
