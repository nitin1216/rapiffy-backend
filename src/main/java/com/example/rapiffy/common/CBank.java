package com.example.rapiffy.common;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CBank {

    private String nameOnCard;

    private String merchantType; // retail, eCommerce, wholesale

    private String bankAccountNumber;

    private String ifsc;
}
