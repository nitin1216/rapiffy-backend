package com.example.rapiffy.repos;

import com.example.rapiffy.model.profiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<profiles, Long> {

    // Used to fetch profile by linked user id
    Optional<profiles> findByUserId(Long userId);
}
