package com.example.rapiffy.services;

import com.example.rapiffy.model.RefreshToken;
import com.example.rapiffy.model.User;

public interface RefreshTokenService {
    RefreshToken create(User user);
    RefreshToken validate(String token);
    void deleteByUser(User user);
}
