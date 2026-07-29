package com.krushna.moviebooking.booking.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Primary component implementation of {@link ShowClient}.
 * Connects to Show Service (or simulates inter-service call with in-memory seat catalog state).
 */
@Slf4j
@Component
public class DefaultShowClient implements ShowClient {

    private final Map<UUID, List<ShowSeatDto>> showSeatCatalog = new ConcurrentHashMap<>();

    @Override
    public boolean existsShow(UUID showId) {
        log.debug("Checking existence of showId: {}", showId);
        return showId != null;
    }

    @Override
    public Optional<ShowDto> getShowById(UUID showId) {
        log.debug("Fetching show details for showId: {}", showId);
        if (showId == null) {
            return Optional.empty();
        }
        return Optional.of(new ShowDto(
                showId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SCHEDULED"
        ));
    }

    @Override
    public List<ShowSeatDto> getShowSeatsByIds(UUID showId, List<UUID> showSeatIds) {
        log.debug("Fetching show seats for showId: {}, seatIds: {}", showId, showSeatIds);
        if (showId == null || showSeatIds == null || showSeatIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ShowSeatDto> catalogSeats = showSeatCatalog.get(showId);
        if (catalogSeats == null) {
            // Default generated seats for simulation / testing when catalog is unpopulated
            List<ShowSeatDto> generated = new ArrayList<>();
            for (int i = 0; i < showSeatIds.size(); i++) {
                UUID seatId = showSeatIds.get(i);
                generated.add(new ShowSeatDto(
                        seatId,
                        showId,
                        seatId,
                        "A" + (i + 1),
                        new BigDecimal("250.00"),
                        "AVAILABLE"
                ));
            }
            return generated;
        }

        return catalogSeats.stream()
                .filter(seat -> showSeatIds.contains(seat.id()))
                .toList();
    }

    @Override
    public void updateShowSeatsStatus(UUID showId, List<UUID> showSeatIds, String status) {
        log.info("Updating show seats status for showId: {}, seatIds: {}, targetStatus: {}",
                showId, showSeatIds, status);
        List<ShowSeatDto> current = showSeatCatalog.getOrDefault(showId, new ArrayList<>());
        List<ShowSeatDto> updated = new ArrayList<>(current);

        for (UUID seatId : showSeatIds) {
            updated.removeIf(s -> s.id().equals(seatId));
            updated.add(new ShowSeatDto(
                    seatId,
                    showId,
                    seatId,
                    "A1",
                    new BigDecimal("250.00"),
                    status
            ));
        }
        showSeatCatalog.put(showId, updated);
    }
}
