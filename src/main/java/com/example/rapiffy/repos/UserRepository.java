package com.example.rapiffy.repos;

import com.example.rapiffy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used at signup: check if phone already registered
    boolean existsByPhoneNumber(String phoneNumber);

    // Used at login: find user by phone to verify password
    Optional<User> findByPhoneNumber(String phoneNumber);
}
