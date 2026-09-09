package com.example.rapiffy.repos;

import com.example.rapiffy.enums.ReturnStatus;
import com.example.rapiffy.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    // All return requests by a customer (newest first)
    List<ReturnRequest> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    // All return requests for a specific sub-order
    List<ReturnRequest> findByOrderId(Long orderId);

    // Check if any return request exists for a sub-order (prevent re-submission)
    boolean existsByOrderId(Long orderId);

    // All return requests for orders belonging to a shop (Admin view)
    List<ReturnRequest> findByOrderShopIdOrderByCreatedAtDesc(Long shopId);

    // Filter by status for Admin (e.g. only REQUESTED ones)
    List<ReturnRequest> findByOrderShopIdAndStatusOrderByCreatedAtDesc(Long shopId, ReturnStatus status);
}
