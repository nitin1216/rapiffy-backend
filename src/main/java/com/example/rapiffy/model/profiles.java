package com.example.rapiffy.model;

import com.example.rapiffy.common.CAddress;
import com.example.rapiffy.common.CName;
import com.example.rapiffy.common.CPhone;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "profiles")
@Data
public class profiles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Embedded
    private CName fullName;

    @Embedded
    private CAddress address;

    @Embedded
    private CPhone phoneNumber;
}
