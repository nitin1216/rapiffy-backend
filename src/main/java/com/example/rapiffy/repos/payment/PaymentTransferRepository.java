package com.example.rapiffy.repos.payment;

import com.example.rapiffy.model.payment.PaymentTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransferRepository extends JpaRepository<PaymentTransfer, Long> {

    // Find by Razorpay transfer ID (used in webhook when transfer settles)
    Optional<PaymentTransfer> findByRazorpayTransferId(String razorpayTransferId);

    // All transfers for a payment
    List<PaymentTransfer> findByPaymentId(Long paymentId);

    // Transfer for a specific sub-order
    Optional<PaymentTransfer> findByOrderId(Long orderId);
}
