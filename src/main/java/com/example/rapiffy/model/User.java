package com.example.rapiffy.model;

import java.time.LocalDateTime;

import com.example.rapiffy.enums.AuthProvider;
import com.example.rapiffy.enums.Roles;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    @Column(name = "phone_number", unique = true, nullable = true)
    private String phoneNumber;

    // Nullable — Google users have no password
    @Column(nullable = true)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Roles role;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider;

    // Google's unique user ID (sub claim) — null for NORMAL users
    @Column(name = "google_sub", unique = true)
    private String googleSub;

    // Whether the email is verified — true for Google, false for NORMAL signup
    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    // Profile picture URL from Google — null for NORMAL users
    @Column(name = "picture")
    private String picture;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
