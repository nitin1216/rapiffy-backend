package com.example.rapiffy.repos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.rapiffy.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used at signup: check if phone already registered
    boolean existsByPhoneNumber(String phoneNumber);

    // Used at login: find user by phone to verify password
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Used at Google login: find by Google's unique user ID
    Optional<User> findByGoogleSub(String googleSub);

    // Used at Google login: fallback find by email
    Optional<User> findByEmail(String email);
}
