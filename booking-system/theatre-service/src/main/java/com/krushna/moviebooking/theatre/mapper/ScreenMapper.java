package com.krushna.moviebooking.theatre.mapper;

import com.krushna.moviebooking.theatre.dto.ScreenRequest;
import com.krushna.moviebooking.theatre.dto.ScreenResponse;
import com.krushna.moviebooking.theatre.entity.Screen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for Screen domain conversions.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {SeatMapper.class})
public interface ScreenMapper {

    @Mapping(target = "theatreId", source = "theatre.id")
    @Mapping(target = "seats", source = "seats")
    ScreenResponse toResponse(Screen screen);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "theatre", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "seats", ignore = true)
    Screen toEntity(ScreenRequest request);
}
