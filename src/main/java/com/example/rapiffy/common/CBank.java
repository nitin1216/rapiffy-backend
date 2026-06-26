package com.example.rapiffy.common;

import jakarta.persistence.*;

public class CBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private String nameOnCard;

    @Embedded
    private String merchantType; // retails, eCommerce, Wholesale

    @Embedded
    private String bankAccountNumber;

    @Embedded
    private String ifsc;
}
