package com.example.rapiffy.model;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CBank;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.common.CPhone;
import com.example.rapiffy.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "profiles")
@Data
public class profiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

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

    @Column(name = "serving_range_km")
    private String servingRangeInKm; // coverage range (km) to deliver the product

    @Embedded
    private CBank bankDetails;

    @Column(name = "gst_number", length = 15)
    private String gstNumber;

    @Column(name = "subscription_start_date")
    private LocalDate subscriptionStartDate;

    @Column(name = "subscription_end_date")
    private LocalDate subscriptionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private SubscriptionStatus subscriptionStatus;

    @Column(name = "no_of_delivery_persons")
    private Long noOfDeliveryPersons;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dialCode",    column = @Column(name = "profile_dial_code")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "profile_phone_number"))
    })
    private CPhone phoneNumber;
}
