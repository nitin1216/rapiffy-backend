package com.example.rapiffy.repos;

import com.example.rapiffy.model.User;
import com.example.rapiffy.model.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByCustomer(User customer);
}
