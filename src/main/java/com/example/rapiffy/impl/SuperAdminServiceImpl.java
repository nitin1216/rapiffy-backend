package com.example.rapiffy.impl;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CBank;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.dto.admin.AdminProfileResponse;
import com.example.rapiffy.dto.superadmin.*;
import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.enums.CategoryType;
import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.exceptions.ApiException;
import com.example.rapiffy.model.*;
import com.example.rapiffy.repos.*;
import com.example.rapiffy.services.SuperAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private final CategoryRepository categoryRepository;
    private final SubCategoryRepository subCategoryRepository;
    private final MasterProductRepository masterProductRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ShopProductRepository shopProductRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public SuperAdminServiceImpl(CategoryRepository categoryRepository,
                                 SubCategoryRepository subCategoryRepository,
                                 MasterProductRepository masterProductRepository,
                                 UserRepository userRepository,
                                 ProfileRepository profileRepository,
                                 ShopProductRepository shopProductRepository) {
        this.categoryRepository = categoryRepository;
        this.subCategoryRepository = subCategoryRepository;
        this.masterProductRepository = masterProductRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.shopProductRepository = shopProductRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // ── SUBCATEGORY MANAGEMENT ────────────────────────────────────────────────

    @Override
    public SuperAdminActionResponse createSubCategory(CreateSubCategoryRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        if (subCategoryRepository.existsByNameAndCategory(request.getName(), category)) {
            throw new ApiException("SubCategory '" + request.getName() + "' already exists under this category", HttpStatus.CONFLICT);
        }

        SubCategory subCategory = new SubCategory();
        subCategory.setName(request.getName());
        subCategory.setImageUrl(request.getImageUrl());
        subCategory.setDescription(request.getDescription());
        subCategory.setCategory(category);
        subCategory.setActive(true);

        subCategoryRepository.save(subCategory);
        return new SuperAdminActionResponse("SubCategory '" + request.getName() + "' created under '" + category.getCategoryName() + "'");
    }

    @Override
    public List<SubCategoryResponse> getSubCategories(Long categoryId) {
        List<SubCategory> list = categoryId != null
            ? subCategoryRepository.findByCategory(
                categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND)))
            : subCategoryRepository.findAll();
        return list.stream().map(SubCategoryResponse::from).collect(Collectors.toList());
    }

    @Override
    public SuperAdminActionResponse deactivateSubCategory(Long subCategoryId) {
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));
        subCategory.setActive(false);
        subCategoryRepository.save(subCategory);
        return new SuperAdminActionResponse("SubCategory '" + subCategory.getName() + "' deactivated");
    }

    // ── CATEGORY MANAGEMENT ──────────────────────────────────────────────────

    @Override
    public SuperAdminActionResponse createCategory(CreateCategoryRequest request) {
        Category category = new Category();
        category.setCategoryType(request.getCategoryType());
        category.setImageUrl(request.getImageUrl());
        category.setDescription(request.getDescription());
        category.setActive(true);

        categoryRepository.save(category);
        return new SuperAdminActionResponse("Category '" + request.getCategoryType().display() + "' created successfully");
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public SuperAdminActionResponse deactivateCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        category.setActive(false);
        categoryRepository.save(category);
        return new SuperAdminActionResponse("Category '" + category.getCategoryName() + "' deactivated");
    }

    @Override
    public SuperAdminActionResponse addMasterProduct(AddMasterProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));
        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));
        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new ApiException("SubCategory does not belong to the given Category", HttpStatus.BAD_REQUEST);
        }

        if (masterProductRepository.existsByProductCode(request.getProductCode())) {
            throw new ApiException("Product code '" + request.getProductCode() + "' already exists", HttpStatus.CONFLICT);
        }

        MasterProduct mp = new MasterProduct();
        mp.setSubCategory(subCategory);
        mp.setProductCode(request.getProductCode());
        mp.setProductName(request.getProductName());
        mp.setBrand(request.getBrand());
        mp.setUnit(request.getUnit());
        mp.setUnitValue(request.getUnitValue());
        mp.setMrp(request.getMrp());
        mp.setImageUrl(request.getImageUrl());
        mp.setShortDescription(request.getShortDescription());
        mp.setLongDescription(request.getLongDescription());
        mp.setHasVariants(request.isHasVariants());
        mp.setActive(true);

        if (request.getVariants() != null && !request.getVariants().isEmpty()) {
            mp.setHasVariants(true);
            syncMasterVariants(mp, request.getVariants());
        }

        masterProductRepository.save(mp);
        pushToMatchingShops(List.of(mp), subCategory);
        return new SuperAdminActionResponse("Master product '" + mp.getProductName() + "' added successfully");
    }

    @Override
    public List<MasterProductResponse> getAllMasterProducts() {
        return masterProductRepository.findAll()
            .stream().map(MasterProductResponse::from).collect(Collectors.toList());
    }

    // ── CSV IMPORT ───────────────────────────────────────────────────────────

    /**
     * CSV format expected:
     * productCode,productName,brand,unit,unitValue,mrp,shortDescription,longDescription,attributes
     *
     * Example row:
     * GRO-001,Basmati Rice,India Gate,KG,5,500,Premium aged rice,Long grain basmati...,{"organic":true}
     *
     * - First row is header (skipped)
     * - Duplicate productCodes are skipped
     * - Empty/invalid rows are skipped
     */
    @Override
    public CsvImportResponse importCatalog(Long categoryId, Long subCategoryId, MultipartFile file) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));
        SubCategory subCategory = subCategoryRepository.findById(subCategoryId)
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));
        if (!subCategory.getCategory().getId().equals(category.getId())) {
            throw new ApiException("SubCategory does not belong to the given Category", HttpStatus.BAD_REQUEST);
        }

        List<MasterProduct> productsToSave = new ArrayList<>();
        int totalRows = 0;
        int skipped = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            boolean isHeader = true;

            while ((line = reader.readLine()) != null) {
                if (isHeader) { isHeader = false; continue; }

                totalRows++;
                String[] columns = parseCsvLine(line);

                if (columns.length < 2 || columns[0].isBlank() || columns[1].isBlank()) {
                    skipped++;
                    continue;
                }

                String productCode = columns[0].trim();
                if (masterProductRepository.existsByProductCode(productCode)) {
                    skipped++;
                    continue;
                }

                MasterProduct product = new MasterProduct();
                product.setProductCode(productCode);
                product.setProductName(columns[1].trim());
                product.setSubCategory(subCategory);
                product.setBrand(getColumn(columns, 2));
                product.setUnit(getColumn(columns, 3));
                product.setUnitValue(getColumn(columns, 4));
                product.setMrp(parseDouble(getColumn(columns, 5)));
                product.setShortDescription(getColumn(columns, 6));
                product.setLongDescription(getColumn(columns, 7));
                product.setActive(true);

                productsToSave.add(product);
            }
        } catch (Exception e) {
            throw new ApiException("Failed to parse CSV: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        masterProductRepository.saveAll(productsToSave);
        pushToMatchingShops(productsToSave, subCategory);

        int imported = productsToSave.size();
        return new CsvImportResponse(
            totalRows, imported, skipped,
            "Imported " + imported + " products into '" + subCategory.getName() + "'"
        );
    }

    // ── UPDATE MASTER PRODUCT (with variants) ────────────────────────────────

    @Override
    public SuperAdminActionResponse updateMasterProduct(Long masterProductId, UpdateMasterProductRequest request) {
        MasterProduct mp = masterProductRepository.findById(masterProductId)
            .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        // Update basic fields (only non-null)
        if (request.getProductName() != null) mp.setProductName(request.getProductName());
        if (request.getBrand() != null) mp.setBrand(request.getBrand());
        if (request.getUnit() != null) mp.setUnit(request.getUnit());
        if (request.getUnitValue() != null) mp.setUnitValue(request.getUnitValue());
        if (request.getMrp() != null) mp.setMrp(request.getMrp());
        if (request.getImageUrl() != null) mp.setImageUrl(request.getImageUrl());
        if (request.getShortDescription() != null) mp.setShortDescription(request.getShortDescription());
        if (request.getLongDescription() != null) mp.setLongDescription(request.getLongDescription());

        // Handle variants
        if (request.getHasVariants() != null) {
            mp.setHasVariants(request.getHasVariants());
        }

        if (request.getVariants() != null) {
            syncMasterVariants(mp, request.getVariants());
        }

        masterProductRepository.save(mp);
        return new SuperAdminActionResponse("Master product '" + mp.getProductName() + "' updated successfully");
    }

    @Override
    public SuperAdminActionResponse addProductToShop(AddProductToShopRequest request) {
        User user = userRepository.findByPhoneNumber(request.getAdminPhone())
            .orElseThrow(() -> new ApiException("Admin not found with phone: " + request.getAdminPhone(), HttpStatus.NOT_FOUND));

        if (user.getRole() != Roles.ADMIN)
            throw new ApiException("User is not an Admin", HttpStatus.BAD_REQUEST);

        Profile profile = profileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException("Profile not found", HttpStatus.NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ApiException("Category not found", HttpStatus.NOT_FOUND));

        SubCategory subCategory = subCategoryRepository.findById(request.getSubCategoryId())
            .orElseThrow(() -> new ApiException("SubCategory not found", HttpStatus.NOT_FOUND));

        if (!subCategory.getCategory().getId().equals(category.getId()))
            throw new ApiException("SubCategory does not belong to the given Category", HttpStatus.BAD_REQUEST);

        MasterProduct mp = masterProductRepository.findById(request.getMasterProductId())
            .orElseThrow(() -> new ApiException("Master product not found", HttpStatus.NOT_FOUND));

        if (!mp.getSubCategory().getId().equals(subCategory.getId()))
            throw new ApiException("Product does not belong to the given SubCategory", HttpStatus.BAD_REQUEST);

        boolean categoryBelongsToShop = profile.getShopCategories().stream()
            .anyMatch(c -> c.getId().equals(category.getId()));
        if (!categoryBelongsToShop)
            throw new ApiException("Category '" + category.getCategoryName() + "' is not assigned to this shop", HttpStatus.BAD_REQUEST);

        if (shopProductRepository.findByShopAndMasterProduct(profile, mp).isPresent())
            throw new ApiException("Product already exists in this shop", HttpStatus.CONFLICT);

        ShopProduct sp = new ShopProduct();
        sp.setShop(profile);
        sp.setMasterProduct(mp);
        sp.setSubCategory(subCategory);
        sp.setProductName(mp.getProductName());
        sp.setBrand(mp.getBrand());
        sp.setUnit(mp.getUnit());
        sp.setUnitValue(mp.getUnitValue());
        sp.setMrp(mp.getMrp());
        sp.setImageUrl(mp.getImageUrl());
        sp.setShortDescription(mp.getShortDescription());
        sp.setLongDescription(mp.getLongDescription());
        sp.setHasVariants(mp.isHasVariants());
        sp.setSellingPrice(0.0);
        sp.setStockQuantity(0);
        sp.setActive(true);

        shopProductRepository.save(sp);
        return new SuperAdminActionResponse("Product '" + mp.getProductName() + "' added to shop '" + profile.getShopName() + "'");
    }

    @Override
    public AdminProfileResponse getAdminProfile(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new ApiException("Admin not found", HttpStatus.NOT_FOUND));
        if (user.getRole() != Roles.ADMIN)
            throw new ApiException("User is not an Admin", HttpStatus.BAD_REQUEST);
        Profile profile = profileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException("Profile not found", HttpStatus.NOT_FOUND));
        return buildAdminProfileResponse(profile, user);
    }

    @Override
    public List<AdminProfileResponse> getAllAdmins() {
        return profileRepository.findAllByUserRole(Roles.ADMIN).stream()
            .map(p -> buildAdminProfileResponse(p, p.getUser()))
            .collect(Collectors.toList());
    }

    // ── ADMIN ONBOARDING & REMOVAL ──────────────────────────────────────────

    @Override
    public SuperAdminActionResponse onboardAdmin(OnboardAdminRequest request) {
        // Check if phone already registered
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ApiException("Phone number already registered", HttpStatus.CONFLICT);
        }

        // Create User with ADMIN role
        User user = new User();
        user.setPhoneNumber(request.getPhoneNumber());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Roles.ADMIN);
        user.setAuthProvider(AuthProvider.NORMAL);
        User savedUser = userRepository.save(user);

        // Fetch categories by enum types
        List<Category> categories = categoryRepository.findByCategoryTypeIn(request.getCategoryTypes());
        if (categories.isEmpty()) {
            throw new ApiException("No valid categories found", HttpStatus.BAD_REQUEST);
        }

        // Auto-set editUnlistedProducts = true if CLOTH category is assigned
        boolean hasCloth = request.getCategoryTypes().contains(CategoryType.CLOTH);

        // Create Profile with shop details
        Profile profile = new Profile();
        profile.setUser(savedUser);
        profile.setShopName(request.getShopName());
        profile.setShopCategories(new LinkedHashSet<>(categories));
        profile.setServingRangeInKm(request.getServingRangeInKm());
        profile.setGstNumber(request.getGstNumber());
        profile.setEditUnlistedProducts(hasCloth);

        // Set bank details
        if (request.getBankAccountNumber() != null) {
            com.example.rapiffy.common.CBank bank = new com.example.rapiffy.common.CBank();
            bank.setNameOnCard(request.getNameOnCard());
            bank.setMerchantType(request.getMerchantType());
            bank.setBankAccountNumber(request.getBankAccountNumber());
            bank.setIfsc(request.getIfsc());
            profile.setBankDetails(bank);
        }

        profileRepository.save(profile);

        // Bulk-add all active MasterProducts for assigned categories as ShopProducts
        if (request.isAddAllProducts()) {
            List<SubCategory> subCategories = subCategoryRepository.findAll().stream()
                .filter(sc -> categories.stream().anyMatch(c -> c.getId().equals(sc.getCategory().getId())))
                .collect(Collectors.toList());
            List<MasterProduct> masterProducts = masterProductRepository.findBySubCategoryInAndIsActiveTrue(subCategories);
            List<ShopProduct> shopProducts = masterProducts.stream().map(mp -> {
                ShopProduct sp = new ShopProduct();
                sp.setShop(profile);
                sp.setMasterProduct(mp);
                sp.setSubCategory(mp.getSubCategory());
                sp.setProductName(mp.getProductName());
                sp.setShortDescription(mp.getShortDescription());
                sp.setLongDescription(mp.getLongDescription());
                sp.setBrand(mp.getBrand());
                sp.setUnit(mp.getUnit());
                sp.setUnitValue(mp.getUnitValue());
                sp.setMrp(mp.getMrp());
                sp.setImageUrl(mp.getImageUrl());
                sp.setSellingPrice(0.0);
                sp.setStockQuantity(0);
                sp.setHasVariants(mp.isHasVariants());
                sp.setActive(true);
                return sp;
            }).collect(Collectors.toList());
            shopProductRepository.saveAll(shopProducts);
        }

        return new SuperAdminActionResponse(
            "Admin '" + request.getShopName() + "' onboarded with " + categories.size() + " categories"
        );
    }

    @Override
    public SuperAdminActionResponse updateAdminProfile(String phoneNumber, UpdateAdminProfileBySuperAdminRequest request) {
        User user = userRepository.findByPhoneNumber(phoneNumber)
            .orElseThrow(() -> new ApiException("Admin not found with phone: " + phoneNumber, HttpStatus.NOT_FOUND));

        if (user.getRole() != Roles.ADMIN) {
            throw new ApiException("User is not an Admin", HttpStatus.BAD_REQUEST);
        }

        Profile profile = profileRepository.findByUserId(user.getId())
            .orElseThrow(() -> new ApiException("Profile not found", HttpStatus.NOT_FOUND));

        // Name
        CName name = profile.getFullName() != null ? profile.getFullName() : new CName();
        if (request.getPrefix() != null) name.setPrefix(request.getPrefix());
        if (request.getFirstName() != null) name.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null) name.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null) name.setLastName(request.getLastName());
        if (request.getSuffix() != null) name.setSuffix(request.getSuffix());
        profile.setFullName(name);

        // Address
        CAddress address = profile.getAddress() != null ? profile.getAddress() : new CAddress();
        if (request.getPinCode() != null) address.setPinCode(request.getPinCode());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1());
        if (request.getLatitude() != null) address.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) address.setLongitude(request.getLongitude());
        profile.setAddress(address);

        // Personal fields
        if (request.getDob() != null) profile.setDob(request.getDob());
        if (request.getPan() != null) profile.setPan(request.getPan());
        if (request.getAadhaar() != null) profile.setAadhaar(request.getAadhaar());

        // Shop details
        if (request.getShopName() != null) profile.setShopName(request.getShopName());
        if (request.getServingRangeInKm() != null) profile.setServingRangeInKm(request.getServingRangeInKm());
        if (request.getGstNumber() != null) profile.setGstNumber(request.getGstNumber());
        if (request.getNoOfDeliveryPersons() != null) profile.setNoOfDeliveryPersons(request.getNoOfDeliveryPersons());

        // Categories — additive merge by enum type, no duplicates
        if (request.getCategoryTypes() != null && !request.getCategoryTypes().isEmpty()) {
            List<Category> newCategories = categoryRepository.findByCategoryTypeIn(request.getCategoryTypes());
            if (newCategories.isEmpty()) throw new ApiException("No valid categories found", HttpStatus.BAD_REQUEST);
            newCategories.forEach(c -> {
                if (!profile.getShopCategories().contains(c)) profile.getShopCategories().add(c);
            });
            // Auto-set editUnlistedProducts if CLOTH is in the new or existing categories
            boolean hasCloth = profile.getShopCategories().stream()
                .anyMatch(c -> c.getCategoryType() == CategoryType.CLOTH);
            profile.setEditUnlistedProducts(hasCloth);
        }

        // Manual override of editUnlistedProducts by SUPERADMIN (takes precedence)
        if (request.getEditUnlistedProducts() != null) {
            profile.setEditUnlistedProducts(request.getEditUnlistedProducts());
        }

        // Bank details (SuperAdmin exclusive)
        CBank bank = profile.getBankDetails() != null ? profile.getBankDetails() : new CBank();
        if (request.getNameOnCard() != null) bank.setNameOnCard(request.getNameOnCard());
        if (request.getMerchantType() != null) bank.setMerchantType(request.getMerchantType());
        if (request.getBankAccountNumber() != null) bank.setBankAccountNumber(request.getBankAccountNumber());
        if (request.getIfsc() != null) bank.setIfsc(request.getIfsc());
        profile.setBankDetails(bank);

        // Sync email — skip if already taken by another user
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            boolean emailTaken = userRepository.findByEmail(request.getEmail())
                .filter(u -> !u.getId().equals(user.getId()))
                .isPresent();
            if (emailTaken) throw new ApiException("Email already in use", HttpStatus.CONFLICT);
            user.setEmail(request.getEmail());
            userRepository.save(user);
        }

        profileRepository.save(profile);
        return new SuperAdminActionResponse("Admin profile updated successfully");
    }

    @Override
    public SuperAdminActionResponse removeAdmin(Long adminUserId) {
        User user = userRepository.findById(adminUserId)
            .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (user.getRole() != Roles.ADMIN) {
            throw new ApiException("User is not an Admin", HttpStatus.BAD_REQUEST);
        }

        // Delete profile (shop) and user
        profileRepository.findByUserId(adminUserId).ifPresent(profileRepository::delete);
        userRepository.delete(user);

        return new SuperAdminActionResponse("Admin (userId: " + adminUserId + ") removed successfully");
    }

    // ── HELPERS ──────────────────────────────────────────────────────────────

    private AdminProfileResponse buildAdminProfileResponse(Profile profile, User user) {
        AdminProfileResponse r = new AdminProfileResponse();
        r.setProfileId(profile.getId());
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
        r.setShopName(profile.getShopName());
        r.setShopCategories(profile.getShopCategories().stream()
            .map(c -> c.getCategoryName()).distinct().collect(Collectors.toList()));
        r.setServingRangeInKm(profile.getServingRangeInKm());
        r.setGstNumber(profile.getGstNumber());
        r.setNoOfDeliveryPersons(profile.getNoOfDeliveryPersons());
        r.setEditUnlistedProducts(profile.isEditUnlistedProducts());
        CBank bank = profile.getBankDetails();
        if (bank != null) {
            r.setNameOnCard(bank.getNameOnCard());
            r.setMerchantType(bank.getMerchantType());
            r.setMaskedAccountNumber(maskLast(bank.getBankAccountNumber(), 4));
            r.setMaskedIfsc(maskLast(bank.getIfsc(), 3));
        }
        r.setSubscriptionStartDate(profile.getSubscriptionStartDate());
        r.setSubscriptionEndDate(profile.getSubscriptionEndDate());
        r.setSubscriptionStatus(profile.getSubscriptionStatus() != null
            ? profile.getSubscriptionStatus().name() : null);
        return r;
    }

    private void pushToMatchingShops(List<MasterProduct> products, SubCategory subCategory) {
        Long categoryId = subCategory.getCategory().getId();
        List<Profile> shops = profileRepository.findAllByShopCategoryId(categoryId);
        if (shops.isEmpty()) return;

        List<ShopProduct> toSave = new ArrayList<>();
        for (Profile shop : shops) {
            for (MasterProduct mp : products) {
                if (shopProductRepository.findByShopAndMasterProduct(shop, mp).isPresent()) continue;
                ShopProduct sp = new ShopProduct();
                sp.setShop(shop);
                sp.setMasterProduct(mp);
                sp.setSubCategory(subCategory);
                sp.setProductName(mp.getProductName());
                sp.setBrand(mp.getBrand());
                sp.setUnit(mp.getUnit());
                sp.setUnitValue(mp.getUnitValue());
                sp.setMrp(mp.getMrp());
                sp.setImageUrl(mp.getImageUrl());
                sp.setShortDescription(mp.getShortDescription());
                sp.setLongDescription(mp.getLongDescription());
                sp.setHasVariants(mp.isHasVariants());
                sp.setSellingPrice(0.0);
                sp.setStockQuantity(0);
                sp.setActive(true);
                toSave.add(sp);
            }
        }
        shopProductRepository.saveAll(toSave);
    }

    private String maskLast(String value, int lastN) {
        if (value == null || value.length() <= lastN) return value;
        return "x".repeat(value.length() - lastN) + value.substring(value.length() - lastN);
    }

    private String getColumn(String[] columns, int index) {
        if (index < columns.length && !columns[index].isBlank()) {
            return columns[index].trim();
        }
        return null;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Simple CSV line parser that handles quoted fields with commas.
     * e.g. "Hello, World",value2 → ["Hello, World", "value2"]
     */
    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString());
        return result.toArray(new String[0]);
    }

    /**
     * Sync master product variants:
     * - Has id → update existing
     * - No id → add new
     * - Missing from list → remove
     */
    private void syncMasterVariants(MasterProduct mp, List<MasterVariantRequest> variantRequests) {
        Map<Long, MasterProductVariant> existingMap = mp.getVariants().stream()
            .collect(Collectors.toMap(MasterProductVariant::getId, v -> v));

        List<MasterProductVariant> updatedList = new ArrayList<>();

        for (MasterVariantRequest vr : variantRequests) {
            if (vr.getId() != null && existingMap.containsKey(vr.getId())) {
                // Update existing
                MasterProductVariant existing = existingMap.get(vr.getId());
                if (vr.getVariantName() != null) existing.setVariantName(vr.getVariantName());
                if (vr.getBrand() != null) existing.setBrand(vr.getBrand());
                if (vr.getUnit() != null) existing.setUnit(vr.getUnit());
                if (vr.getUnitValue() != null) existing.setUnitValue(vr.getUnitValue());
                if (vr.getMrp() != null) existing.setMrp(vr.getMrp());
                if (vr.getImageUrl() != null) existing.setImageUrl(vr.getImageUrl());
                updatedList.add(existing);
            } else {
                // Add new
                MasterProductVariant newVariant = new MasterProductVariant();
                newVariant.setMasterProduct(mp);
                newVariant.setVariantName(vr.getVariantName());
                newVariant.setBrand(vr.getBrand());
                newVariant.setUnit(vr.getUnit());
                newVariant.setUnitValue(vr.getUnitValue());
                newVariant.setMrp(vr.getMrp());
                newVariant.setImageUrl(vr.getImageUrl());
                newVariant.setActive(true);
                updatedList.add(newVariant);
            }
        }

        // Replace — orphanRemoval handles DB delete for removed ones
        mp.getVariants().clear();
        mp.getVariants().addAll(updatedList);
        mp.setHasVariants(!updatedList.isEmpty());
    }
}
