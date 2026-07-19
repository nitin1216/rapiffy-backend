package com.example.rapiffy.impl.customer;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.dto.customer.*;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.CustomerAddress;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.CustomerAddressRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.customer.CustomerProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final UserRepository userRepository;
    private final CustomerAddressRepository addressRepository;

    @Override
    public CustomerProfileResponse getProfile(Long userId) {
        User user = getUser(userId);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public CustomerProfileResponse updateProfile(Long userId, UpdateCustomerProfileRequest request) {
        User user = getUser(userId);
        CName name = user.getFullName() != null ? user.getFullName() : new CName();
        if (request.getPrefix() != null) name.setPrefix(request.getPrefix());
        if (request.getFirstName() != null) name.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null) name.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null) name.setLastName(request.getLastName());
        if (request.getSuffix() != null) name.setSuffix(request.getSuffix());
        user.setFullName(name);
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        return toProfileResponse(userRepository.save(user));
    }

    @Override
    public List<CustomerAddressResponse> getAddresses(Long userId) {
        User user = getUser(userId);
        return addressRepository.findByCustomer(user)
                .stream().map(this::toAddressResponse).toList();
    }

    @Override
    @Transactional
    public CustomerAddressResponse addAddress(Long userId, SaveAddressRequest request) {
        User user = getUser(userId);

        // If this is the first address or marked as default → clear existing defaults
        boolean isFirst = addressRepository.findByCustomer(user).isEmpty();
        if (request.isDefault() || isFirst) {
            addressRepository.clearDefaultForCustomer(user);
        }

        CustomerAddress address = new CustomerAddress();
        address.setCustomer(user);
        address.setLabel(request.getLabel());
        address.setAddress(buildCAddress(request));
        address.setDefault(request.isDefault() || isFirst);
        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerAddressResponse updateAddress(Long userId, Long addressId, SaveAddressRequest request) {
        User user = getUser(userId);
        CustomerAddress address = getAddress(addressId, user);

        address.setLabel(request.getLabel());
        address.setAddress(buildCAddress(request));

        if (request.isDefault()) {
            addressRepository.clearDefaultForCustomer(user);
            address.setDefault(true);
        }

        return toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        User user = getUser(userId);
        CustomerAddress address = getAddress(addressId, user);

        // If deleting default address → set the next available as default
        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        if (wasDefault) {
            List<CustomerAddress> remaining = addressRepository.findByCustomer(user);
            if (!remaining.isEmpty()) {
                remaining.get(0).setDefault(true);
                addressRepository.save(remaining.get(0));
            }
        }
    }

    @Override
    @Transactional
    public List<CustomerAddressResponse> setDefaultAddress(Long userId, Long addressId) {
        User user = getUser(userId);
        CustomerAddress address = getAddress(addressId, user);

        // Clear all defaults first then set new one
        addressRepository.clearDefaultForCustomer(user);
        address.setDefault(true);
        addressRepository.save(address);

        return addressRepository.findByCustomer(user)
                .stream().map(this::toAddressResponse).toList();
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
    }

    private CustomerAddress getAddress(Long addressId, User user) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ApiException("Address not found", HttpStatus.NOT_FOUND));
        if (!address.getCustomer().getId().equals(user.getId()))
            throw new ApiException("Access denied", HttpStatus.FORBIDDEN);
        return address;
    }

    private CAddress buildCAddress(SaveAddressRequest request) {
        CAddress addr = new CAddress();
        addr.setAddressLine1(request.getAddressLine1());
        addr.setCity(request.getCity());
        addr.setState(request.getState());
        addr.setPinCode(request.getPinCode());
        addr.setCountry(request.getCountry());
        addr.setLatitude(request.getLatitude());
        addr.setLongitude(request.getLongitude());
        return addr;
    }

    private CustomerProfileResponse toProfileResponse(User user) {
        CustomerProfileResponse r = new CustomerProfileResponse();
        r.setUserId(user.getId());
        r.setEmail(user.getEmail());
        r.setPhoneNumber(user.getPhoneNumber());
        CName name = user.getFullName();
        if (name != null) {
            r.setPrefix(name.getPrefix());
            r.setFirstName(name.getFirstName());
            r.setMiddleName(name.getMiddleName());
            r.setLastName(name.getLastName());
            r.setSuffix(name.getSuffix());
        }
        return r;
    }

    private CustomerAddressResponse toAddressResponse(CustomerAddress a) {
        CustomerAddressResponse r = new CustomerAddressResponse();
        r.setAddressId(a.getId());
        r.setLabel(a.getLabel());
        r.setDefault(a.isDefault());
        if (a.getAddress() != null) {
            r.setAddressLine1(a.getAddress().getAddressLine1());
            r.setCity(a.getAddress().getCity());
            r.setState(a.getAddress().getState());
            r.setPinCode(a.getAddress().getPinCode());
            r.setCountry(a.getAddress().getCountry());
            r.setLatitude(a.getAddress().getLatitude());
            r.setLongitude(a.getAddress().getLongitude());
        }
        return r;
    }
}
