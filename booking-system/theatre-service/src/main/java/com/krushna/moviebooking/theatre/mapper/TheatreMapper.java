package com.krushna.moviebooking.theatre.mapper;

import com.krushna.moviebooking.theatre.dto.TheatreRequest;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;
import com.krushna.moviebooking.theatre.entity.Theatre;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Theatre domain conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {CityMapper.class, ScreenMapper.class})
public interface TheatreMapper {

    @Mapping(target = "city", source = "city")
    @Mapping(target = "screens", source = "screens")
    TheatreResponse toResponse(Theatre theatre);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "screens", ignore = true)
    Theatre toEntity(TheatreRequest request);
}
