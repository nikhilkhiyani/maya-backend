package com.MAYA.studio.service;

import com.MAYA.studio.dto.AddressRequest;
import com.MAYA.studio.dto.AddressResponse;
import com.MAYA.studio.entity.Address;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.AddressMapper;
import com.MAYA.studio.repository.AddressRepository;
import com.MAYA.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailOrPhone(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public List<AddressResponse> getMyAddresses() {
        User user = getCurrentUser();
        return addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user).stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(AddressRequest request) {
        User user = getCurrentUser();

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                    .forEach(a -> a.setIsDefault(false));
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() :
                        addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user).isEmpty())
                .build();

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(UUID id, AddressRequest request) {
        User user = getCurrentUser();
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.findByUserOrderByIsDefaultDescCreatedAtDesc(user)
                    .forEach(a -> a.setIsDefault(false));
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        return addressMapper.toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID id) {
        User user = getCurrentUser();
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    public Address getAddressForUser(UUID addressId, User user) {
        return addressRepository.findByIdAndUser(addressId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
    }
}
