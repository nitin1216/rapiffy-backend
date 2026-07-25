package com.example.rapiffy.repos.payment;

import com.example.rapiffy.enums.PaymentStatus;
import com.example.rapiffy.model.payment.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Find by Razorpay order ID (used in webhook when payment is captured)
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    // Find by Razorpay payment ID
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    // Find by parent order ID
    Optional<Payment> findByParentOrderId(Long parentOrderId);

    // Find stale PENDING payments older than a given time (for reconciliation)
    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime before);
}
