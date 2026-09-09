package com.example.rapiffy.repos;

import com.example.rapiffy.model.DeliveryLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliveryLocationRepository extends JpaRepository<DeliveryLocation, Long> {

    Optional<DeliveryLocation> findByOrderId(Long orderId);
}
