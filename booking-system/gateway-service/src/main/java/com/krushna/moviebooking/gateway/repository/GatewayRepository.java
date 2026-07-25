package com.krushna.moviebooking.gateway.repository;

import com.krushna.moviebooking.gateway.entity.GatewayEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GatewayRepository extends JpaRepository<GatewayEntity, Long> {}
