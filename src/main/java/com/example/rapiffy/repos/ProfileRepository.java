package com.example.rapiffy.repos;

import com.example.rapiffy.enums.Roles;
import com.example.rapiffy.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    @Query("SELECT p FROM Profile p WHERE p.user.role = :role")
    List<Profile> findAllByUserRole(Roles role);
}
