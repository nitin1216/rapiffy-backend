package com.example.rapiffy.repos;

import com.example.rapiffy.model.CustomerAddress;
import com.example.rapiffy.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByCustomer(User customer);

    Optional<CustomerAddress> findByCustomerAndIsDefault(User customer, boolean isDefault);

    // Reset all addresses of a customer to non-default before setting a new default
    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer = :customer")
    void clearDefaultForCustomer(User customer);
}
