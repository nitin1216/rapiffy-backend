package com.example.rapiffy.dto;

import com.example.rapiffy.enums.Roles;

import lombok.Data;
@Data
public class SignUpRequest {
    private String phoneNumber;
    private String password;
    private Roles role; // we can not ask role to any user
}
