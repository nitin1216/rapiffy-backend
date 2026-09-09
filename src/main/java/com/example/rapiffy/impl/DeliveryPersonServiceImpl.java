package com.example.rapiffy.impl;

import com.example.rapiffy.common.CName;
import com.example.rapiffy.dto.admin.DeliveryPersonResponse;
import com.example.rapiffy.dto.delivery.OnboardDeliveryPersonRequest;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.enums.DeliveryBoyOnboardedBy;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.DeliveryPerson;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import com.example.rapiffy.repos.DeliveryPersonRepository;
import com.example.rapiffy.repos.ProfileRepository;
import com.example.rapiffy.repos.UserRepository;
import com.example.rapiffy.services.DeliveryPersonService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliveryPersonServiceImpl implements DeliveryPersonService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final DeliveryPersonRepository deliveryPersonRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DeliveryPersonServiceImpl(UserRepository userRepository,
                                     ProfileRepository profileRepository,
                                     DeliveryPersonRepository deliveryPersonRepository) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.deliveryPersonRepository = deliveryPersonRepository;
    }

    @Override
    @Transactional
    public DeliveryPersonResponse onboardByAdmin(Long adminUserId, OnboardDeliveryPersonRequest request) {
        Profile shop = profileRepository.findByUserId(adminUserId)
                .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
        return onboard(shop, request, DeliveryBoyOnboardedBy.BY_ADMIN);
    }

    @Override
    @Transactional
    public DeliveryPersonResponse onboardBySuperAdmin(Long shopProfileId, OnboardDeliveryPersonRequest request) {
        Profile shop = profileRepository.findById(shopProfileId)
                .orElseThrow(() -> new ApiException("Shop profile not found", HttpStatus.NOT_FOUND));
        if (shop.getUser().getRole() != Roles.ADMIN)
            throw new ApiException("Target profile is not a shop", HttpStatus.BAD_REQUEST);
        return onboard(shop, request, DeliveryBoyOnboardedBy.BY_SUPERADMIN);
    }

    @Override
    public List<DeliveryPersonResponse> getDeliveryPersonsByShop(Long shopProfileId) {
        return deliveryPersonRepository.findByShopId(shopProfileId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public DeliveryPersonResponse deactivate(Long deliveryPersonId) {
        DeliveryPerson dp = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new ApiException("Delivery person not found", HttpStatus.NOT_FOUND));
        dp.setActive(false);
        return toResponse(deliveryPersonRepository.save(dp));
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private DeliveryPersonResponse onboard(Profile shop, OnboardDeliveryPersonRequest request, DeliveryBoyOnboardedBy onboardedBy) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber()))
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);

        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.DELIVERY);
        user.setAuthProvider(AuthProvider.NORMAL);

        CName name = new CName();
        name.setFirstName(request.getFirstName());
        name.setLastName(request.getLastName());
        user.setFullName(name);

        User savedUser = userRepository.save(user);

        DeliveryPerson dp = new DeliveryPerson();
        dp.setUser(savedUser);
        dp.setShop(shop);
        dp.setOnboardedBy(onboardedBy);
        dp.setActive(true);

        return toResponse(deliveryPersonRepository.save(dp));
    }

    public DeliveryPersonResponse toResponse(DeliveryPerson dp) {
        DeliveryPersonResponse r = new DeliveryPersonResponse();
        r.setDeliveryPersonId(dp.getId());
        r.setUserId(dp.getUser().getId());
        r.setPhoneNumber(dp.getUser().getPhoneNumber());
        r.setOnboardedBy(dp.getOnboardedBy());
        r.setActive(dp.isActive());
        CName name = dp.getUser().getFullName();
        if (name != null) {
            r.setFirstName(name.getFirstName());
            r.setLastName(name.getLastName());
        }
        return r;
    }
}
