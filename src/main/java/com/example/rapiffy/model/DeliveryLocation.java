package com.example.rapiffy.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Stores the LATEST live location of a delivery person for a given order.
 * Only one record per order — updated in place on every push.
 */
@Entity
@Table(name = "delivery_locations")
@Data
public class DeliveryLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // GPS accuracy in meters (optional)
    @Column
    private Double accuracy;

    // Device battery level 0-100 (optional, for adaptive update frequency)
    @Column(name = "battery_level")
    private Integer batteryLevel;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
