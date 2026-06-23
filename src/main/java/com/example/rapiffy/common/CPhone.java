package com.example.rapiffy.common;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CPhone {

    private String dialCode;
    private String phoneNumber;
}
