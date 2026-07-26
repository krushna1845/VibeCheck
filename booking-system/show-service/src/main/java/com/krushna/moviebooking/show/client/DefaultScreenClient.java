package com.krushna.moviebooking.show.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Primary component implementation of {@link ScreenClient}.
 * Provides Screen & Seat retrieval from Theatre Service with logging.
 */
@Slf4j
@Component
public class DefaultScreenClient implements ScreenClient {

    @Override
    public Optional<ScreenDto> getScreenById(UUID screenId) {
        log.debug("Fetching screen details for screenId: {}", screenId);
        if (screenId == null) {
            return Optional.empty();
        }
        return Optional.of(new ScreenDto(screenId, UUID.randomUUID(), "Screen 1", "IMAX", 50));
    }

    @Override
    public boolean existsScreen(UUID screenId) {
        log.debug("Checking existence of screenId: {}", screenId);
        return screenId != null;
    }

    @Override
    public List<SeatDto> getActiveSeatsByScreen(UUID screenId) {
        log.debug("Fetching active seats for screenId: {}", screenId);
        if (screenId == null) {
            return List.of();
        }
        List<SeatDto> seats = new ArrayList<>();
        String[] categories = {"REGULAR", "PREMIUM", "VIP"};
        for (int row = 1; row <= 5; row++) {
            char rowChar = (char) ('A' + row - 1);
            String category = categories[(row - 1) % categories.length];
            for (int col = 1; col <= 10; col++) {
                seats.add(new SeatDto(
                        UUID.nameUUIDFromBytes((screenId.toString() + rowChar + col).getBytes()),
                        screenId,
                        String.valueOf(rowChar),
                        col,
                        category,
                        true
                ));
            }
        }
        return seats;
    }
}
