package com.krushna.moviebooking.auth.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class AuthEntity {
    @Id
    private Long id;
}
