package com.example.rapiffy.impl;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CBank;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.admin.UpdateAdminProfileRequest;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.AdminProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
public class AdminProfileServiceImpl implements AdminProfileService {

    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;

    public AdminProfileServiceImpl(ProfileRepository profileRepository,
                                   UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public AdminProfileResponse getProfile(Long userId) {
        Profile profile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Profile not found", HttpStatus.NOT_FOUND));
        User user = profile.getUser();
        return buildResponse(profile, user);
    }

    @Override
    @Transactional
    public AdminProfileResponse updateProfile(Long userId, UpdateAdminProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
            .orElseThrow(() -> new ApiException("Profile not found", HttpStatus.NOT_FOUND));
        User user = profile.getUser();

        // Update name
        CName name = profile.getFullName() != null ? profile.getFullName() : new CName();
        if (request.getPrefix() != null) name.setPrefix(request.getPrefix());
        if (request.getFirstName() != null) name.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null) name.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null) name.setLastName(request.getLastName());
        if (request.getSuffix() != null) name.setSuffix(request.getSuffix());
        profile.setFullName(name);

        // Update address
        CAddress address = profile.getAddress() != null ? profile.getAddress() : new CAddress();
        if (request.getPinCode() != null) address.setPinCode(request.getPinCode());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1());
        if (request.getLatitude() != null) address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) address.setLongitude(request.getLongitude());
        profile.setAddress(address);

        // Update personal fields
        if (request.getDob() != null) profile.setDob(request.getDob());
        if (request.getPan() != null) profile.setPan(request.getPan());
        if (request.getAadhaar() != null) profile.setAadhaar(request.getAadhaar());

        // Update shop details
        if (request.getShopName() != null) profile.setShopName(request.getShopName());
        if (request.getServingRangeInKm() != null) profile.setServingRangeInKm(request.getServingRangeInKm());
        if (request.getGstNumber() != null) profile.setGstNumber(request.getGstNumber());
        if (request.getNoOfDeliveryPersons() != null) profile.setNoOfDeliveryPersons(request.getNoOfDeliveryPersons());

        // Sync email to User table
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            user.setEmail(request.getEmail());
            userRepository.save(user);
        }

        profileRepository.save(profile);
        return buildResponse(profile, user);
    }

    private AdminProfileResponse buildResponse(Profile profile, User user) {
        AdminProfileResponse r = new AdminProfileResponse();
        r.setProfileId(profile.getId());

        // Name
        CName name = profile.getFullName();
        if (name != null) {
            r.setPrefix(name.getPrefix());
            r.setFirstName(name.getFirstName());
            r.setMiddleName(name.getMiddleName());
            r.setLastName(name.getLastName());
            r.setSuffix(name.getSuffix());
        }

        r.setEmail(user.getEmail());
        r.setPhoneNumber(user.getPhoneNumber());
        r.setDob(profile.getDob());
        r.setPan(profile.getPan());
        r.setAadhaar(profile.getAadhaar());

        // Address
        CAddress addr = profile.getAddress();
        if (addr != null) {
            r.setPinCode(addr.getPinCode());
            r.setState(addr.getState());
            r.setCity(addr.getCity());
            r.setCountry(addr.getCountry());
            r.setAddressLine1(addr.getAddressLine1());
            r.setLatitude(addr.getLatitude());
            r.setLongitude(addr.getLongitude());
        }

        // Shop
        r.setShopName(profile.getShopName());
        r.setShopCategories(profile.getShopCategories().stream()
            .map(c -> c.getCategoryName())
            .collect(Collectors.toList()));
        r.setServingRangeInKm(profile.getServingRangeInKm());
        r.setGstNumber(profile.getGstNumber());
        r.setNoOfDeliveryPersons(profile.getNoOfDeliveryPersons());

        // Bank (masked)
        CBank bank = profile.getBankDetails();
        if (bank != null) {
            r.setNameOnCard(bank.getNameOnCard());
            r.setMerchantType(bank.getMerchantType());
            r.setMaskedAccountNumber(maskValue(bank.getBankAccountNumber(), 4));
            r.setMaskedIfsc(maskValue(bank.getIfsc(), 3));
        }

        // Subscription
        r.setSubscriptionStartDate(profile.getSubscriptionStartDate());
        r.setSubscriptionEndDate(profile.getSubscriptionEndDate());
        r.setSubscriptionStatus(profile.getSubscriptionStatus() != null
            ? profile.getSubscriptionStatus().name() : null);

        return r;
    }

    /**
     * Masks all characters except last N digits.
     * e.g. "123456789012" with lastN=4 → "xxxxxxxx9012"
     */
    private String maskValue(String value, int lastN) {
        if (value == null || value.length() <= lastN) return value;
        String visible = value.substring(value.length() - lastN);
        String masked = "x".repeat(value.length() - lastN);
        return masked + visible;
    }
}
