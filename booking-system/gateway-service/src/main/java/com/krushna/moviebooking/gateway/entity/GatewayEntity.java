package com.krushna.moviebooking.gateway.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class GatewayEntity {
    @Id
    private Long id;
}
