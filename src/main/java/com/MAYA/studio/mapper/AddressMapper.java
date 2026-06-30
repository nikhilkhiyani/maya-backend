package com.MAYA.studio.mapper;

import com.MAYA.studio.dto.AddressResponse;
import com.MAYA.studio.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {
    AddressResponse toResponse(Address address);
}
