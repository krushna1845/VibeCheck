package com.krushna.moviebooking.booking.service;

import com.krushna.moviebooking.booking.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service interface governing the complete lifecycle of movie ticket bookings.
 *
 * <p>Provides methods for booking creation, patch updates, payment confirmation,
 * cancellation, reservation expiration, soft-deletion, and pageable queries.
 */
public interface BookingService {

    /**
     * Creates a new movie ticket booking in PENDING status.
     * Validates seat availability, acquires temporary Redis locks, computes price/tax/fee,
     * persists the booking aggregate, and emits a {@code BookingCreatedEvent}.
     *
     * @param request Booking creation request payload
     * @return Detailed {@link BookingResponse}
     * @throws com.krushna.moviebooking.booking.exception.SeatUnavailableException if selected seats are locked or booked
     */
    BookingResponse createBooking(BookingRequest request);

    /**
     * Updates an existing active booking using patch semantics.
     * Only non-null fields in {@code request} will update the underlying entity.
     *
     * @param id Primary UUID of the booking
     * @param request Patch update request payload
     * @return Updated {@link BookingResponse}
     * @throws com.krushna.moviebooking.booking.exception.BookingNotFoundException if no active booking exists with ID
     */
    BookingResponse updateBooking(UUID id, BookingUpdateRequest request);

    /**
     * Confirms a pending booking upon receiving a successful payment callback.
     * Updates booking status to CONFIRMED, mutates ShowSeats to BOOKED in Show Service,
     * releases temporary Redis seat locks, and emits a {@code BookingConfirmedEvent}.
     * Operation is idempotent: repeated calls for an already confirmed booking return cleanly.
     *
     * @param bookingReference Unique 12-character booking reference code
     * @param paymentId Transaction ID from payment gateway
     * @return Confirmed {@link BookingResponse}
     * @throws com.krushna.moviebooking.booking.exception.BookingNotFoundException if reference is not found
     * @throws com.krushna.moviebooking.booking.exception.BookingExpiredException if reservation expired
     */
    BookingResponse confirmBooking(String bookingReference, String paymentId);

    /**
     * Cancels an existing booking, restores seat availability, releases Redis locks,
     * and emits a {@code BookingCancelledEvent}.
     *
     * @param bookingReference Unique 12-character booking reference code
     * @param reason Cancellation rationale
     * @return Cancelled {@link BookingResponse}
     */
    BookingResponse cancelBooking(String bookingReference, String reason);

    /**
     * Expires an unconfirmed reservation whose TTL has passed.
     * Sets status to EXPIRED, releases seat locks, and emits a {@code BookingExpiredEvent}.
     *
     * @param id Booking primary UUID
     */
    void expireBooking(UUID id);

    /**
     * Soft-deletes a booking by setting its {@code deletedAt} timestamp.
     *
     * @param id Booking primary UUID
     */
    void deleteBooking(UUID id);

    /**
     * Retrieves detailed booking response by primary UUID.
     *
     * @param id Booking primary UUID
     * @return Detailed {@link BookingResponse}
     */
    BookingResponse getBookingById(UUID id);

    /**
     * Retrieves detailed booking response by unique 12-character reference code.
     *
     * @param bookingReference Booking reference code
     * @return Detailed {@link BookingResponse}
     */
    BookingResponse getBookingByReference(String bookingReference);

    /**
     * Retrieves a page of all active bookings formatted as summary objects.
     *
     * @param pageable Page request parameters
     * @return Page of {@link BookingSummary}
     */
    Page<BookingSummary> getAllBookings(Pageable pageable);

    /**
     * Retrieves a page of bookings for a specific customer user.
     *
     * @param userId Customer user UUID
     * @param pageable Page request parameters
     * @return Page of {@link BookingSummary}
     */
    Page<BookingSummary> getUserBookings(UUID userId, Pageable pageable);

    /**
     * Retrieves confirmed bookings for a specific show.
     *
     * @param showId Show UUID
     * @return List of {@link BookingSummary}
     */
    List<BookingSummary> getShowBookings(UUID showId);
}
