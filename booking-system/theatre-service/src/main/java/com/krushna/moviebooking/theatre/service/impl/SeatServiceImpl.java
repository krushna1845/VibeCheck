package com.krushna.moviebooking.theatre.service.impl;

import com.krushna.moviebooking.theatre.dto.SeatRequest;
import com.krushna.moviebooking.theatre.dto.SeatResponse;
import com.krushna.moviebooking.theatre.entity.Screen;
import com.krushna.moviebooking.theatre.entity.Seat;
import com.krushna.moviebooking.theatre.entity.Theatre;
import com.krushna.moviebooking.theatre.exception.*;
import com.krushna.moviebooking.theatre.mapper.SeatMapper;
import com.krushna.moviebooking.theatre.repository.ScreenRepository;
import com.krushna.moviebooking.theatre.repository.SeatRepository;
import com.krushna.moviebooking.theatre.service.SeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of {@link SeatService}.
 *
 * Enforces business rules:
 * - Seat numbers must be unique inside one screen.
 * - Capacity must equal the number of active seats.
 * - Soft delete only (deactivates seat).
 * - Seat category/type validation.
 * - Transactional boundaries & structured logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private static final Set<String> ALLOWED_SEAT_CATEGORIES =
            Set.of("REGULAR", "PREMIUM", "VIP", "BALCONY", "RECLINER", "EXECUTIVE");

    private final SeatRepository seatRepository;
    private final ScreenRepository screenRepository;
    private final SeatMapper seatMapper;

    @Override
    @Transactional
    public SeatResponse createSeat(UUID screenId, SeatRequest request) {
        log.info("Creating seat row '{}' number {} for screen id: {}", request.seatRow(), request.seatNumber(), screenId);

        Screen screen = findActiveScreenAndTheatreOrThrow(screenId);

        validateUniqueSeatNumber(screenId, request.seatRow(), request.seatNumber());
        validateSeatCategory(request.seatCategory());

        Seat seat = seatMapper.toEntity(request);
        seat.setScreen(screen);
        seat.setSeatRow(request.seatRow().trim().toUpperCase());
        seat.setSeatCategory(request.seatCategory().trim().toUpperCase());
        if (request.isActive() != null) {
            seat.setIsActive(request.isActive());
        }

        Seat saved = seatRepository.save(seat);
        syncScreenCapacity(screen);

        log.info("Seat created successfully with id: {}", saved.getId());
        return seatMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public List<SeatResponse> createSeatsBatch(UUID screenId, List<SeatRequest> requests) {
        log.info("Creating batch of {} seats for screen id: {}", requests.size(), screenId);

        Screen screen = findActiveScreenAndTheatreOrThrow(screenId);

        Set<String> uniqueKeys = new HashSet<>();
        List<Seat> seatsToSave = new ArrayList<>();

        for (SeatRequest req : requests) {
            String key = req.seatRow().trim().toUpperCase() + "_" + req.seatNumber();
            if (!uniqueKeys.add(key)) {
                throw new DuplicateSeatException(req.seatRow(), req.seatNumber(), screenId);
            }

            validateUniqueSeatNumber(screenId, req.seatRow(), req.seatNumber());
            validateSeatCategory(req.seatCategory());

            Seat seat = seatMapper.toEntity(req);
            seat.setScreen(screen);
            seat.setSeatRow(req.seatRow().trim().toUpperCase());
            seat.setSeatCategory(req.seatCategory().trim().toUpperCase());
            if (req.isActive() != null) {
                seat.setIsActive(req.isActive());
            }
            seatsToSave.add(seat);
        }

        List<Seat> savedSeats = seatRepository.saveAll(seatsToSave);
        syncScreenCapacity(screen);

        log.info("Successfully created {} seats for screen id: {}", savedSeats.size(), screenId);
        return savedSeats.stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(UUID id, SeatRequest request) {
        log.info("Updating seat id: {}", id);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new SeatNotFoundException(id));

        Screen screen = findActiveScreenAndTheatreOrThrow(seat.getScreen().getId());

        if (request.seatRow() != null && request.seatNumber() != null) {
            String newRow = request.seatRow().trim().toUpperCase();
            if (!seat.getSeatRow().equalsIgnoreCase(newRow) || !seat.getSeatNumber().equals(request.seatNumber())) {
                validateUniqueSeatNumber(screen.getId(), newRow, request.seatNumber());
                seat.setSeatRow(newRow);
                seat.setSeatNumber(request.seatNumber());
            }
        }

        if (request.seatCategory() != null) {
            validateSeatCategory(request.seatCategory());
            seat.setSeatCategory(request.seatCategory().trim().toUpperCase());
        }

        if (request.isActive() != null) {
            seat.setIsActive(request.isActive());
        }

        syncScreenCapacity(screen);
        log.info("Seat updated successfully: id={}", id);
        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatResponse getSeatById(UUID id) {
        log.debug("Fetching seat by id: {}", id);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new SeatNotFoundException(id));

        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByScreen(UUID screenId) {
        log.debug("Fetching all seats for screen id: {}", screenId);

        findActiveScreenAndTheatreOrThrow(screenId);

        return seatRepository.findByScreenId(screenId).stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getActiveSeatsByScreen(UUID screenId) {
        log.debug("Fetching active seats for screen id: {}", screenId);

        findActiveScreenAndTheatreOrThrow(screenId);

        return seatRepository.findByScreenIdAndIsActiveTrue(screenId).stream()
                .map(seatMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SeatResponse toggleSeatStatus(UUID id, boolean isActive) {
        log.info("Toggling seat status for id={} to isActive={}", id, isActive);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new SeatNotFoundException(id));

        Screen screen = findActiveScreenAndTheatreOrThrow(seat.getScreen().getId());
        seat.setIsActive(isActive);

        syncScreenCapacity(screen);
        log.info("Seat id={} isActive toggled to {}", id, isActive);
        return seatMapper.toResponse(seat);
    }

    @Override
    @Transactional
    public void deleteSeat(UUID id) {
        log.info("Soft-deleting (deactivating) seat id: {}", id);

        Seat seat = seatRepository.findById(id)
                .orElseThrow(() -> new SeatNotFoundException(id));

        Screen screen = findActiveScreenAndTheatreOrThrow(seat.getScreen().getId());
        seat.setIsActive(false);

        syncScreenCapacity(screen);
        log.info("Seat soft-deleted (deactivated) successfully: id={}", id);
    }

    private void syncScreenCapacity(Screen screen) {
        long activeCount = seatRepository.countByScreenIdAndIsActiveTrue(screen.getId());
        screen.setTotalSeats((int) activeCount);
        screenRepository.save(screen);
    }

    private void validateUniqueSeatNumber(UUID screenId, String seatRow, Integer seatNumber) {
        if (seatRepository.existsByScreenIdAndSeatRowIgnoreCaseAndSeatNumber(screenId, seatRow.trim(), seatNumber)) {
            throw new DuplicateSeatException(seatRow, seatNumber, screenId);
        }
    }

    private void validateSeatCategory(String category) {
        if (category == null || !ALLOWED_SEAT_CATEGORIES.contains(category.trim().toUpperCase())) {
            throw new InvalidSeatTypeException(category);
        }
    }

    private Screen findActiveScreenAndTheatreOrThrow(UUID screenId) {
        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ScreenNotFoundException(screenId));

        Theatre theatre = screen.getTheatre();
        if (theatre.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(theatre.getStatus())) {
            throw new InactiveTheatreException(theatre.getId());
        }
        return screen;
    }
}
