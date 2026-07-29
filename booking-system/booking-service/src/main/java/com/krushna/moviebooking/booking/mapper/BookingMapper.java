package com.krushna.moviebooking.booking.mapper;

import com.krushna.moviebooking.booking.dto.BookingResponse;
import com.krushna.moviebooking.booking.dto.BookingSummary;
import com.krushna.moviebooking.booking.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.util.List;

/**
 * MapStruct mapper for converting between {@link Booking} entity and outbound DTOs.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {BookingSeatMapper.class})
public interface BookingMapper {

    @Mapping(target = "seats", source = "bookingSeats")
    BookingResponse toResponse(Booking booking);

    List<BookingResponse> toResponseList(List<Booking> bookings);

    @Mapping(target = "seatCount", source = "booking", qualifiedByName = "calculateSeatCount")
    BookingSummary toSummary(Booking booking);

    List<BookingSummary> toSummaryList(List<Booking> bookings);

    @Named("calculateSeatCount")
    default int calculateSeatCount(Booking booking) {
        return booking.getBookingSeats() != null ? booking.getBookingSeats().size() : 0;
    }
}
