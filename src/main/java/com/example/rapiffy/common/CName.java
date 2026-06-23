package com.example.rapiffy.common;

import jakarta.persistence.Embeddable;
import lombok.Data;

@Embeddable
@Data
public class CName {

    private String prefix;
    private String firstName;
    private String middleName;
    private String lastName;
    private String suffix;
}
