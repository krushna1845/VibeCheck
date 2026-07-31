package com.krushna.moviebooking.booking.service.impl;

import com.krushna.moviebooking.booking.client.PaymentClient;
import com.krushna.moviebooking.booking.client.ShowClient;
import com.krushna.moviebooking.booking.dto.*;
import com.krushna.moviebooking.booking.entity.Booking;
import com.krushna.moviebooking.booking.entity.BookingSeat;
import com.krushna.moviebooking.booking.event.*;
import com.krushna.moviebooking.booking.exception.*;
import com.krushna.moviebooking.booking.mapper.BookingMapper;
import com.krushna.moviebooking.booking.repository.BookingRepository;
import com.krushna.moviebooking.booking.repository.BookingSpecification;
import com.krushna.moviebooking.booking.service.BookingService;
import com.krushna.moviebooking.booking.service.SeatLockService;
import com.krushna.moviebooking.booking.validator.BookingValidationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link BookingService}.
 *
 * <p><b>Transaction boundaries</b>:
 * <ul>
 *   <li>All write methods are {@code @Transactional} with default propagation
 *       (REQUIRED) — joining an existing TX or creating a new one.</li>
 *   <li>All read methods are {@code @Transactional(readOnly = true)} — flushes no
 *       dirty checks and enables database replica routing.</li>
 * </ul>
 *
 * <p><b>Validation strategy</b>:
 * Bean validation fires at controller boundary via {@code @Valid}.
 * Business rules (show existence, seat availability, concurrency lock, state transition)
 * are validated in this layer via {@link BookingValidationFacade} before persistent writes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<String> VALID_STATUSES =
            Set.of("PENDING", "CONFIRMED", "CANCELLED", "EXPIRED");

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");
    private static final BigDecimal DEFAULT_CONVENIENCE_FEE = new BigDecimal("30.00");
    private static final long RESERVATION_TTL_MINUTES = 5;

    private final BookingRepository bookingRepository;
    private final BookingValidationFacade bookingValidationFacade;
    private final SeatLockService seatLockService;
    private final ShowClient showClient;
    private final PaymentClient paymentClient;
    private final BookingEventPublisher bookingEventPublisher;
    private final BookingMapper bookingMapper;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        log.info("Creating booking for userId: {}, showId: {}, seats: {}",
                request.userId(), request.showId(), request.showSeatIds());

        bookingValidationFacade.validateBookingCreation(request);

        String bookingReference = generateBookingReference();

        // 1. Acquire Redis seat locks
        SeatLockRequest lockRequest = SeatLockRequest.builder()
                .showId(request.showId())
                .seatIds(request.showSeatIds())
                .userId(request.userId())
                .bookingReference(bookingReference)
                .ttlSeconds(RESERVATION_TTL_MINUTES * 60)
                .build();

        SeatLockResponse lockResponse = seatLockService.lockSeats(lockRequest);
        if (!lockResponse.success()) {
            log.warn("Seat locking conflict for showId: {}", request.showId());
            throw new SeatUnavailableException(request.showId(), request.showSeatIds());
        }

        // 2. Compute pricing
        List<ShowClient.ShowSeatDto> seatDtos = showClient.getShowSeatsByIds(request.showId(), request.showSeatIds());
        BigDecimal subtotal = seatDtos.stream()
                .map(ShowClient.ShowSeatDto::price)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal taxAmount = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal convenienceFee = DEFAULT_CONVENIENCE_FEE;
        BigDecimal totalAmount = subtotal.add(taxAmount).add(convenienceFee);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofMinutes(RESERVATION_TTL_MINUTES));

        // 3. Construct Booking aggregate
        Booking booking = Booking.builder()
                .bookingReference(bookingReference)
                .userId(request.userId())
                .showId(request.showId())
                .totalAmount(totalAmount)
                .taxAmount(taxAmount)
                .convenienceFee(convenienceFee)
                .status("PENDING")
                .expiresAt(expiresAt)
                .bookingSeats(new ArrayList<>())
                .build();

        for (ShowClient.ShowSeatDto seatDto : seatDtos) {
            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .showSeatId(seatDto.id())
                    .seatNumber(seatDto.seatNumber())
                    .price(seatDto.price())
                    .build();
            booking.getBookingSeats().add(bookingSeat);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created successfully with reference: {} and id: {}", bookingReference, saved.getId());

        // 4. Publish Domain Event
        BookingCreatedEvent createdEvent = BookingCreatedEvent.builder()
                .bookingId(saved.getId())
                .bookingReference(bookingReference)
                .userId(saved.getUserId())
                .showId(saved.getShowId())
                .showSeatIds(request.showSeatIds())
                .totalAmount(totalAmount)
                .expiresAt(expiresAt)
                .timestamp(now)
                .build();
        bookingEventPublisher.publishBookingCreated(createdEvent);

        return bookingMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // UPDATE (Patch Semantics)
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookingResponse updateBooking(UUID id, BookingUpdateRequest request) {
        log.info("Updating booking with id: {}", id);

        Booking booking = findActiveBookingOrThrow(id);

        if (StringUtils.hasText(request.status())) {
            validateStatus(request.status());
            booking.setStatus(request.status().trim().toUpperCase());
        }
        if (request.totalAmount() != null) {
            booking.setTotalAmount(request.totalAmount());
        }
        if (request.taxAmount() != null) {
            booking.setTaxAmount(request.taxAmount());
        }
        if (request.convenienceFee() != null) {
            booking.setConvenienceFee(request.convenienceFee());
        }
        if (request.expiresAt() != null) {
            booking.setExpiresAt(request.expiresAt());
        }

        log.info("Booking updated successfully: id={}", id);
        return bookingMapper.toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // CONFIRM
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookingResponse confirmBooking(String bookingReference, String paymentId) {
        log.info("Confirming booking reference: {}, paymentId: {}", bookingReference, paymentId);

        Booking booking = findActiveBookingByReferenceOrThrow(bookingReference);

        if ("CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            log.info("Booking reference {} is already CONFIRMED. Returning idempotently.", bookingReference);
            return bookingMapper.toResponse(booking);
        }

        if ("EXPIRED".equalsIgnoreCase(booking.getStatus())) {
            throw new BookingExpiredException(bookingReference);
        }

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new BookingAlreadyCancelledException(bookingReference);
        }

        booking.setStatus("CONFIRMED");

        List<UUID> showSeatIds = booking.getBookingSeats().stream()
                .map(BookingSeat::getShowSeatId)
                .toList();

        List<String> seatNumbers = booking.getBookingSeats().stream()
                .map(BookingSeat::getSeatNumber)
                .toList();

        showClient.updateShowSeatsStatus(booking.getShowId(), showSeatIds, "BOOKED");
        seatLockService.releaseLocks(booking.getShowId(), showSeatIds);

        log.info("Booking reference {} confirmed successfully.", bookingReference);

        BookingConfirmedEvent confirmedEvent = BookingConfirmedEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(booking.getBookingReference())
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .showSeatIds(showSeatIds)
                .seatNumbers(seatNumbers)
                .totalAmount(booking.getTotalAmount())
                .paymentId(paymentId)
                .timestamp(Instant.now())
                .build();
        bookingEventPublisher.publishBookingConfirmed(confirmedEvent);

        return bookingMapper.toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // CANCEL
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public BookingResponse cancelBooking(String bookingReference, String reason) {
        log.info("Cancelling booking reference: {}, reason: {}", bookingReference, reason);

        Booking booking = findActiveBookingByReferenceOrThrow(bookingReference);

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            return bookingMapper.toResponse(booking);
        }

        booking.setStatus("CANCELLED");

        List<UUID> showSeatIds = booking.getBookingSeats().stream()
                .map(BookingSeat::getShowSeatId)
                .toList();

        showClient.updateShowSeatsStatus(booking.getShowId(), showSeatIds, "AVAILABLE");
        seatLockService.releaseLocks(booking.getShowId(), showSeatIds);

        BookingCancelledEvent cancelledEvent = BookingCancelledEvent.builder()
                .bookingId(booking.getId())
                .bookingReference(bookingReference)
                .userId(booking.getUserId())
                .showId(booking.getShowId())
                .showSeatIds(showSeatIds)
                .reason(reason != null ? reason : "Customer cancelled")
                .timestamp(Instant.now())
                .build();
        bookingEventPublisher.publishBookingCancelled(cancelledEvent);

        return bookingMapper.toResponse(booking);
    }

    // -------------------------------------------------------------------------
    // EXPIRE
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void expireBooking(UUID id) {
        log.info("Expiring reservation for booking id: {}", id);

        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null || booking.getDeletedAt() != null) {
            return;
        }

        if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("EXPIRED");

            List<UUID> showSeatIds = booking.getBookingSeats().stream()
                    .map(BookingSeat::getShowSeatId)
                    .toList();

            seatLockService.releaseLocks(booking.getShowId(), showSeatIds);

            BookingExpiredEvent expiredEvent = BookingExpiredEvent.builder()
                    .bookingId(booking.getId())
                    .bookingReference(booking.getBookingReference())
                    .userId(booking.getUserId())
                    .showId(booking.getShowId())
                    .showSeatIds(showSeatIds)
                    .timestamp(Instant.now())
                    .build();
            bookingEventPublisher.publishBookingExpired(expiredEvent);

            log.info("Booking ID {} set to EXPIRED", id);
        }
    }

    // -------------------------------------------------------------------------
    // DELETE (Soft Delete)
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteBooking(UUID id) {
        log.info("Soft-deleting booking with id: {}", id);

        Booking booking = findActiveBookingOrThrow(id);
        booking.setDeletedAt(Instant.now());

        log.info("Booking soft-deleted successfully: id={}", id);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(UUID id) {
        log.debug("Fetching booking by id: {}", id);
        Booking booking = findActiveBookingOrThrow(id);
        return bookingMapper.toResponse(booking);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingByReference(String bookingReference) {
        log.debug("Fetching booking by reference: {}", bookingReference);
        Booking booking = findActiveBookingByReferenceOrThrow(bookingReference);
        return bookingMapper.toResponse(booking);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummary> getAllBookings(Pageable pageable) {
        log.debug("Fetching all active bookings, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return bookingRepository.findAll(pageable)
                .map(bookingMapper::toSummary);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<BookingSummary> getUserBookings(UUID userId, Pageable pageable) {
        log.debug("Fetching bookings for userId: {}", userId);
        return bookingRepository.findByUserId(userId, pageable)
                .map(bookingMapper::toSummary);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<BookingSummary> getShowBookings(UUID showId) {
        log.debug("Fetching confirmed bookings for showId: {}", showId);
        List<Booking> bookings = bookingRepository.findByShowIdAndStatus(showId, "CONFIRMED");
        return bookingMapper.toSummaryList(bookings);
    }

    // -------------------------------------------------------------------------
    // Private Helpers & Validations
    // -------------------------------------------------------------------------

    private Booking findActiveBookingOrThrow(UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        if (booking.getDeletedAt() != null) {
            throw new BookingNotFoundException(id);
        }
        return booking;
    }

    private Booking findActiveBookingByReferenceOrThrow(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new BookingNotFoundException(bookingReference));
        if (booking.getDeletedAt() != null) {
            throw new BookingNotFoundException(bookingReference);
        }
        return booking;
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status.trim().toUpperCase())) {
            throw new InvalidBookingStateException("Invalid booking status: " + status);
        }
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }
}
