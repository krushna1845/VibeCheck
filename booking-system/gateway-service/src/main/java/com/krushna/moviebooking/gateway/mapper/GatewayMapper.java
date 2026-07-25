package com.krushna.moviebooking.gateway.mapper;

public interface GatewayMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
