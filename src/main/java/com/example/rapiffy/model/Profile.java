package com.example.rapiffy.model;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CBank;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.common.CPhone;
import com.example.rapiffy.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Profile holds additional details for a User.
 *
 * For ADMIN (shopkeeper):
 * - Contains shop details (name, categories, address, GST, bank, etc.)
 * - shopCategories = list of categories this shop sells (Grocery + Dairy, etc.)
 * - SuperAdmin creates this profile and assigns categories
 *
 * For CUSTOMER / DELIVERY:
 * - Contains personal details (name, address, phone)
 * - shopCategories will be empty
 */
@Entity
@Table(name = "profiles")
@Data
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Linked user account
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // ─── SHOP DETAILS (for Admin/Shopkeeper) ─────────────────────────────────

    // Shop display name (e.g. "Sharma General Store")
    @Column(name = "shop_name")
    private String shopName;

    // One shop can sell multiple categories (Grocery + Dairy + Personal Care)
    // Admin selects these during profile setup
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "shop_categories",
        joinColumns = @JoinColumn(name = "profile_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private List<Category> shopCategories = new ArrayList<>();

    // Delivery coverage radius in kilometers
    @Column(name = "serving_range_km")
    private Double servingRangeInKm;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "no_of_delivery_persons")
    private Integer noOfDeliveryPersons;

    // ─── PERSONAL DETAILS ────────────────────────────────────────────────────

    @Embedded
    private CName fullName;

    @Embedded
    private CAddress address;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "pan", length = 10)
    private String pan;

    @Column(name = "aadhaar", length = 12)
    private String aadhaar;

    // ─── BANK DETAILS ────────────────────────────────────────────────────────

    @Embedded
    private CBank bankDetails;

    // ─── SUBSCRIPTION (managed by SuperAdmin) ────────────────────────────────

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private SubscriptionStatus subscriptionStatus;

    // ─── CONTACT ─────────────────────────────────────────────────────────────

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dialCode", column = @Column(name = "profile_dial_code")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "profile_phone_number"))
    })
    private CPhone phoneNumber;
}
