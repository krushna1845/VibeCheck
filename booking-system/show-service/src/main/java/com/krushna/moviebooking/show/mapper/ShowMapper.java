package com.krushna.moviebooking.show.mapper;

import com.krushna.moviebooking.show.dto.ShowResponse;
import com.krushna.moviebooking.show.entity.Show;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Show} entity and {@link ShowResponse} DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {ShowSeatMapper.class})
public interface ShowMapper {

    @Mapping(target = "seats", source = "showSeats")
    ShowResponse toResponse(Show show);

    List<ShowResponse> toResponseList(List<Show> shows);
}
