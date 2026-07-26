package com.krushna.moviebooking.theatre.mapper;

import com.krushna.moviebooking.theatre.dto.CityRequest;
import com.krushna.moviebooking.theatre.dto.CityResponse;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;
import com.krushna.moviebooking.theatre.entity.City;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for City domain conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CityMapper {

    CityResponse toResponse(City city);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "theatres", ignore = true)
    City toEntity(CityRequest request);

    TheatreResponse.CitySummary toCitySummary(City city);
}
