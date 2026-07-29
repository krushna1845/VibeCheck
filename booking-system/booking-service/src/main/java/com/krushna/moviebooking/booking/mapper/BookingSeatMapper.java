package com.krushna.moviebooking.booking.mapper;

import com.krushna.moviebooking.booking.dto.BookingSeatResponse;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link BookingSeat} entity and {@link BookingSeatResponse} DTO.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BookingSeatMapper {

    BookingSeatResponse toResponse(BookingSeat bookingSeat);

    List<BookingSeatResponse> toResponseList(List<BookingSeat> bookingSeats);
}
