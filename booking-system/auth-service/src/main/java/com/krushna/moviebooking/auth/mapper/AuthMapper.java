package com.krushna.moviebooking.auth.mapper;

public interface AuthMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
