package com.krushna.moviebooking.theatre.service.impl;

import com.krushna.moviebooking.theatre.dto.ScreenRequest;
import com.krushna.moviebooking.theatre.dto.ScreenResponse;
import com.krushna.moviebooking.theatre.dto.SeatRequest;
import com.krushna.moviebooking.theatre.entity.Screen;
import com.krushna.moviebooking.theatre.entity.Seat;
import com.krushna.moviebooking.theatre.entity.Theatre;
import com.krushna.moviebooking.theatre.exception.*;
import com.krushna.moviebooking.theatre.mapper.ScreenMapper;
import com.krushna.moviebooking.theatre.mapper.SeatMapper;
import com.krushna.moviebooking.theatre.repository.ScreenRepository;
import com.krushna.moviebooking.theatre.repository.SeatRepository;
import com.krushna.moviebooking.theatre.repository.TheatreRepository;
import com.krushna.moviebooking.theatre.service.ScreenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Implementation of {@link ScreenService}.
 *
 * Enforces business rules:
 * - Screen names must be unique inside one theatre.
 * - Theatre must be active.
 * - Screen capacity must equal the number of active seats.
 * - Transactional boundaries & structured logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScreenServiceImpl implements ScreenService {

    private static final Set<String> VALID_SEAT_CATEGORIES =
            Set.of("REGULAR", "PREMIUM", "VIP", "BALCONY", "RECLINER", "EXECUTIVE");

    private final ScreenRepository screenRepository;
    private final TheatreRepository theatreRepository;
    private final SeatRepository seatRepository;
    private final ScreenMapper screenMapper;
    private final SeatMapper seatMapper;

    @Override
    @Transactional
    public ScreenResponse createScreen(UUID theatreId, ScreenRequest request) {
        log.info("Creating screen '{}' for theatre id: {}", request.name(), theatreId);

        Theatre theatre = findActiveTheatreOrThrow(theatreId);

        validateUniqueScreenName(theatreId, request.name());

        List<Seat> initialSeats = new ArrayList<>();
        if (request.seats() != null && !request.seats().isEmpty()) {
            validateSeats(request.seats());
            for (SeatRequest seatReq : request.seats()) {
                Seat seat = seatMapper.toEntity(seatReq);
                validateSeatCategory(seat.getSeatCategory());
                initialSeats.add(seat);
            }
        }

        long activeCount = initialSeats.stream().filter(Seat::getIsActive).count();
        int capacity = initialSeats.isEmpty() ? request.totalSeats() : (int) activeCount;

        Screen screen = Screen.builder()
                .theatre(theatre)
                .name(request.name().trim())
                .screenType(StringUtils.hasText(request.screenType()) ? request.screenType().trim() : "STANDARD")
                .totalSeats(capacity)
                .build();

        for (Seat seat : initialSeats) {
            seat.setScreen(screen);
        }
        screen.setSeats(initialSeats);

        Screen saved = screenRepository.save(screen);
        log.info("Screen created successfully with id: {} and totalSeats: {}", saved.getId(), saved.getTotalSeats());
        return screenMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ScreenResponse updateScreen(UUID id, ScreenRequest request) {
        log.info("Updating screen with id: {}", id);

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ScreenNotFoundException(id));

        validateTheatreActive(screen.getTheatre());

        if (StringUtils.hasText(request.name()) && !screen.getName().equalsIgnoreCase(request.name().trim())) {
            validateUniqueScreenName(screen.getTheatre().getId(), request.name());
            screen.setName(request.name().trim());
        }

        if (StringUtils.hasText(request.screenType())) {
            screen.setScreenType(request.screenType().trim());
        }

        if (request.totalSeats() != null) {
            screen.setTotalSeats(request.totalSeats());
        }

        // Recalculate capacity to ensure capacity equals active seats if seats exist
        recalculateAndSetCapacity(screen);

        log.info("Screen updated successfully: id={}", id);
        return screenMapper.toResponse(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenResponse getScreenById(UUID id) {
        log.debug("Fetching screen by id: {}", id);

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ScreenNotFoundException(id));

        return screenMapper.toResponse(screen);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScreenResponse> getScreensByTheatre(UUID theatreId) {
        log.debug("Fetching screens for theatre id: {}", theatreId);

        findActiveTheatreOrThrow(theatreId);

        return screenRepository.findByTheatreId(theatreId).stream()
                .map(screenMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteScreen(UUID id) {
        log.info("Deleting screen with id: {}", id);

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ScreenNotFoundException(id));

        validateTheatreActive(screen.getTheatre());

        // Deactivate all seats and set total capacity to 0
        screen.getSeats().forEach(seat -> seat.setIsActive(false));
        screen.setTotalSeats(0);

        screenRepository.delete(screen);
        log.info("Screen deleted successfully: id={}", id);
    }

    @Override
    @Transactional
    public ScreenResponse updateScreenCapacity(UUID screenId) {
        log.info("Updating capacity for screen id: {}", screenId);

        Screen screen = screenRepository.findById(screenId)
                .orElseThrow(() -> new ScreenNotFoundException(screenId));

        recalculateAndSetCapacity(screen);
        return screenMapper.toResponse(screen);
    }

    private void recalculateAndSetCapacity(Screen screen) {
        long activeSeatsCount = seatRepository.countByScreenIdAndIsActiveTrue(screen.getId());
        screen.setTotalSeats((int) activeSeatsCount);
        log.debug("Screen id={} capacity updated to {}", screen.getId(), activeSeatsCount);
    }

    private void validateUniqueScreenName(UUID theatreId, String name) {
        if (screenRepository.existsByTheatreIdAndNameIgnoreCase(theatreId, name.trim())) {
            throw new DuplicateScreenException(name, theatreId);
        }
    }

    private Theatre findActiveTheatreOrThrow(UUID theatreId) {
        Theatre theatre = theatreRepository.findById(theatreId)
                .orElseThrow(() -> new TheatreNotFoundException(theatreId));

        validateTheatreActive(theatre);
        return theatre;
    }

    private void validateTheatreActive(Theatre theatre) {
        if (theatre.getDeletedAt() != null || !"ACTIVE".equalsIgnoreCase(theatre.getStatus())) {
            throw new InactiveTheatreException(theatre.getId());
        }
    }

    private void validateSeats(List<SeatRequest> seats) {
        Set<String> uniqueRowNum = new HashSet<>();
        for (SeatRequest s : seats) {
            String key = s.seatRow().trim().toUpperCase() + "_" + s.seatNumber();
            if (!uniqueRowNum.add(key)) {
                throw new DuplicateSeatException("Duplicate seat row and number found in payload: " + key);
            }
        }
    }

    private void validateSeatCategory(String category) {
        if (category == null || !VALID_SEAT_CATEGORIES.contains(category.trim().toUpperCase())) {
            throw new InvalidSeatTypeException(category);
        }
    }
}
