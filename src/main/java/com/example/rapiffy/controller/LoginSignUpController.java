package com.example.rapiffy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("v1/login")
public interface LoginSignUpController {

    @GetMapping("/login")
    public ResponseEntity<?> loginByAdmin(String phoneNumber);

    @GetMapping("/admin-sign-up")
    public ResponseEntity<?> signByAdmin(String phoneNumber);

    @GetMapping("/customer-sign-up")
    public ResponseEntity<?> signByCustomer(String phoneNumber);

    @GetMapping("/deli-sign-up")
    public ResponseEntity<?> signByDelivery(String phoneNumber);
}
