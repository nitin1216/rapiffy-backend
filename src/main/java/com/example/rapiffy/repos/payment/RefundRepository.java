package com.example.rapiffy.repos.payment;

import com.example.rapiffy.model.payment.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    // Find by Razorpay refund ID (used in webhook when refund is processed)
    Optional<Refund> findByRazorpayRefundId(String razorpayRefundId);

    // All refunds for a payment (to show customer refund history)
    List<Refund> findByPaymentId(Long paymentId);

    // Refund for a specific sub-order
    Optional<Refund> findByOrderId(Long orderId);
}
