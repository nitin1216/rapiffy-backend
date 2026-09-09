package com.example.rapiffy.repos;

import com.example.rapiffy.model.DeliveryPerson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Long> {
    List<DeliveryPerson> findByShopId(Long shopId);
    Optional<DeliveryPerson> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
