package com.example.rapiffy.services.customer;

import com.example.rapiffy.dto.customer.*;

import java.util.List;

public interface CustomerProfileService {

    CustomerProfileResponse getProfile(Long userId);

    CustomerProfileResponse updateProfile(Long userId, UpdateCustomerProfileRequest request);

    List<CustomerAddressResponse> getAddresses(Long userId);

    CustomerAddressResponse addAddress(Long userId, SaveAddressRequest request);

    CustomerAddressResponse updateAddress(Long userId, Long addressId, SaveAddressRequest request);

    void deleteAddress(Long userId, Long addressId);

    List<CustomerAddressResponse> setDefaultAddress(Long userId, Long addressId);
}
