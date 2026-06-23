package com.example.rapiffy.model;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

//@Entity
//@Table(name= "user")
@Data
public class User {

    BigInteger id;
    String email;
    String phone_number;
    String password;
    BigInteger roleId;
    String authProvider; // Google, Facebook, Normal
    LocalDateTime createdAt;

}
