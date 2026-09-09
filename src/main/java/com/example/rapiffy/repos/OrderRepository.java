package com.example.rapiffy.repos;

import com.example.rapiffy.enums.OrderStatus;
import com.example.rapiffy.model.Order;
import com.example.rapiffy.model.ParentOrder;
import com.example.rapiffy.model.Profile;
import com.example.rapiffy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // All sub-orders under a parent order
    List<Order> findByParentOrder(ParentOrder parentOrder);

    // All orders assigned to a delivery person
    List<Order> findByDeliveryPersonOrderByAssignedAtDesc(User deliveryPerson);

    // All orders assigned to a delivery person filtered by status
    List<Order> findByDeliveryPersonAndStatusOrderByAssignedAtDesc(User deliveryPerson, OrderStatus status);

    // Get specific order belonging to a delivery person (security check)
    Optional<Order> findByIdAndDeliveryPerson(Long id, User deliveryPerson);

    // Counts for delivery stats
    long countByDeliveryPersonAndStatusIn(User deliveryPerson, List<OrderStatus> statuses);
    long countByDeliveryPersonAndStatus(User deliveryPerson, OrderStatus status);

    // Sum of delivery charges earned by a delivery person for all DELIVERED orders
    @Query("SELECT COALESCE(SUM(o.deliveryCharge), 0) FROM Order o WHERE o.deliveryPerson = :dp AND o.status = 'DELIVERED'")
    Double sumDeliveryChargeByDeliveryPersonAndDelivered(@Param("dp") User deliveryPerson);
}
