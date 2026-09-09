package com.example.rapiffy.repos;

import com.example.rapiffy.enums.CancellationStatus;
import com.example.rapiffy.model.CancellationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CancellationRequestRepository extends JpaRepository<CancellationRequest, Long> {

    List<CancellationRequest> findByOrderShopIdOrderByCreatedAtDesc(Long shopId);

    List<CancellationRequest> findByOrderShopIdAndStatusOrderByCreatedAtDesc(Long shopId, CancellationStatus status);

    List<CancellationRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    boolean existsByOrderIdAndStatus(Long orderId, CancellationStatus status);
}
