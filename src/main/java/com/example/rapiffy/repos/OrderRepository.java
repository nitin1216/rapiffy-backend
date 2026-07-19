package com.example.rapiffy.repos;

import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // All orders for a shop (Admin view)
    List<Order> findByShopOrderByCreatedAtDesc(Profile shop);

    // Filter by status (e.g. only PENDING orders)
    List<Order> findByShopAndStatusOrderByCreatedAtDesc(Profile shop, OrderStatus status);

    // Get specific order belonging to a shop (security check)
    Optional<Order> findByIdAndShop(Long id, Profile shop);

    // All orders placed by a customer
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
