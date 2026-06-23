package com.example.rapiffy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignUpResponse {

    private String token;
    private String message;
}
