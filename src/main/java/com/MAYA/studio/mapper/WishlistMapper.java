package com.MAYA.studio.mapper;

import com.MAYA.studio.dto.WishlistResponse;
import com.MAYA.studio.entity.Wishlist;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface WishlistMapper {
    
    WishlistResponse toResponse(Wishlist wishlist);
}
