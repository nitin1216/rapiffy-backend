package com.example.rapiffy.dto;

import com.example.rapiffy.enums.Roles;
import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class SignUpRequest {
    private String phoneNumber;
    private String password;

    // Not exposed in request body — set internally by the controller per endpoint
    @JsonIgnore
    private Roles role;
}
