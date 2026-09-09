package com.example.rapiffy.model;

import com.example.rapiffy.enums.DeliveryBoyOnboardedBy;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_persons")
@Data
public class DeliveryPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // The shop this delivery person is linked to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_profile_id", nullable = false)
    private Profile shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarded_by", nullable = false)
    private DeliveryBoyOnboardedBy onboardedBy;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
