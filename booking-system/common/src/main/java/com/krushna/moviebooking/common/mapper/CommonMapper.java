package com.krushna.moviebooking.common.mapper;

public interface CommonMapper<D, E> {
    D toDto(E entity);
    E toEntity(D dto);
}
