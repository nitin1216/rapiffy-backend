package com.example.rapiffy.impl;

import com.example.rapiffy.controller.LoginSignUpController;
import org.springframework.http.ResponseEntity;

public class LoginSignUpControllerImpl implements LoginSignUpController {
    @Override
    public ResponseEntity<?> loginByAdmin(String phoneNumber) {
        return null;
    }

    @Override
    public ResponseEntity<?> signByAdmin(String phoneNumber) {
        return null;
    }

    @Override
    public ResponseEntity<?> signByCustomer(String phoneNumber) {
        return null;
    }

    @Override
    public ResponseEntity<?> signByDelivery(String phoneNumber) {
        return null;
    }
}
