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

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Embedded
    private CName fullName;

    @Embedded
    private CAddress address;

    @Embedded
    private LocalDate dob;

    @Embedded
    private String pan;

    @Embedded
    private String aadhaar;

    @Embedded
    private String servingRangeInKm; // coverage range (KM) to deliver the product.

    @Embedded
    private CBank bankDetails;

    @Embedded
    private String gstNumber;

    @Embedded
    private LocalDate subscriptionStartDate;

    @Embedded
    private LocalDate subscriptionEndDate;

    @Embedded
    private SubscriptionStatus status;

    @Embedded
    private Long noOfDeliveryPerson;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "dialCode",    column = @Column(name = "profile_dial_code")),
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "profile_phone_number"))
    })
    private CPhone phoneNumber;
}
