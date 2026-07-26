package com.krushna.moviebooking.show.mapper;

import com.krushna.moviebooking.show.dto.ShowSeatResponse;
import com.krushna.moviebooking.show.entity.ShowSeat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link ShowSeat} entity and {@link ShowSeatResponse} DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShowSeatMapper {

    @Mapping(target = "showId", source = "show.id")
    ShowSeatResponse toResponse(ShowSeat showSeat);

    List<ShowSeatResponse> toResponseList(List<ShowSeat> showSeats);
}
