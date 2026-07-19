package com.example.rapiffy.repos;

import com.example.rapiffy.model.ParentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParentOrderRepository extends JpaRepository<ParentOrder, Long> {

    // All parent orders placed by a customer, latest first
    List<ParentOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
