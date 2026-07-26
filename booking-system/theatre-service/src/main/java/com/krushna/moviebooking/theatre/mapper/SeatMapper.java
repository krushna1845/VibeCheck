package com.krushna.moviebooking.theatre.mapper;

import com.krushna.moviebooking.theatre.dto.SeatRequest;
import com.krushna.moviebooking.theatre.dto.SeatResponse;
import com.krushna.moviebooking.theatre.entity.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Seat domain conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SeatMapper {

    @Mapping(target = "screenId", source = "screen.id")
    SeatResponse toResponse(Seat seat);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "screen", ignore = true)
    @Mapping(target = "isActive", source = "isActive", defaultValue = "true")
    Seat toEntity(SeatRequest request);
}
