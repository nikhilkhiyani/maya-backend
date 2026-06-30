package com.MAYA.studio.mapper;

import com.MAYA.studio.dto.OrderResponse;
import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {ProductMapper.class})
public interface OrderMapper {
    
    @Mapping(source = "user.id", target = "userId")
    OrderResponse toResponse(Order order);
    
    @Mapping(source = "product", target = "product")
    OrderResponse.OrderItemResponse toOrderItemResponse(OrderItem orderItem);
}
