package com.krushna.moviebooking.show.service.impl;

import com.krushna.moviebooking.show.client.MovieClient;
import com.krushna.moviebooking.show.client.ScreenClient;
import com.krushna.moviebooking.show.dto.ShowRequest;
import com.krushna.moviebooking.show.dto.ShowResponse;
import com.krushna.moviebooking.show.dto.ShowSeatResponse;
import com.krushna.moviebooking.show.dto.ShowUpdateRequest;
import com.krushna.moviebooking.show.entity.Show;
import com.krushna.moviebooking.show.entity.ShowSeat;
import com.krushna.moviebooking.show.exception.*;
import com.krushna.moviebooking.show.mapper.ShowMapper;
import com.krushna.moviebooking.show.mapper.ShowSeatMapper;
import com.krushna.moviebooking.show.repository.ShowRepository;
import com.krushna.moviebooking.show.repository.ShowSeatRepository;
import com.krushna.moviebooking.show.service.ShowService;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Primary implementation of {@link ShowService}.
 *
 * <p><b>Transaction boundaries</b>:
 * <ul>
 *   <li>All write methods are {@code @Transactional}.</li>
 *   <li>All read methods are {@code @Transactional(readOnly = true)}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final ShowSeatRepository showSeatRepository;
    private final MovieClient movieClient;
    private final ScreenClient screenClient;
    private final ShowMapper showMapper;
    private final ShowSeatMapper showSeatMapper;

    @Setter
    @Value("${show.cleaning-buffer-minutes:30}")
    private int cleaningBufferMinutes = 30;

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShowResponse createShow(ShowRequest request) {
        log.info("Creating show for movieId: {}, screenId: {}, startTime: {}",
                request.movieId(), request.screenId(), request.startTime());

        validateMovieExists(request.movieId());
        validateScreenExists(request.screenId());
        validateStartTime(request.startTime());

        int durationMinutes = movieClient.getMovieDurationMinutes(request.movieId());
        Instant endTime = request.startTime().plus(Duration.ofMinutes(durationMinutes + cleaningBufferMinutes));

        validateNoOverlap(request.screenId(), request.startTime(), endTime, null);

        Show show = Show.builder()
                .movieId(request.movieId())
                .theatreId(request.theatreId())
                .screenId(request.screenId())
                .startTime(request.startTime())
                .endTime(endTime)
                .language(request.language().trim())
                .status("SCHEDULED")
                .showSeats(new ArrayList<>())
                .build();

        List<ScreenClient.SeatDto> activeSeats = screenClient.getActiveSeatsByScreen(request.screenId());
        for (ScreenClient.SeatDto seatDto : activeSeats) {
            BigDecimal price = resolvePrice(seatDto.seatCategory(), request.defaultPrice(), request.categoryPrices());
            ShowSeat showSeat = ShowSeat.builder()
                    .show(show)
                    .seatId(seatDto.id())
                    .price(price)
                    .status("AVAILABLE")
                    .version(0L)
                    .build();
            show.getShowSeats().add(showSeat);
        }

        Show saved = showRepository.save(show);
        log.info("Show created successfully with id: {} and {} seats", saved.getId(), saved.getShowSeats().size());
        return showMapper.toResponse(saved);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShowResponse updateShow(UUID id, ShowUpdateRequest request) {
        log.info("Updating show with id: {}", id);

        Show show = findActiveShowOrThrow(id);
        ensureNotCancelled(show);
        ensureNotStarted(show);

        UUID targetMovieId = request.movieId() != null ? request.movieId() : show.getMovieId();
        UUID targetScreenId = request.screenId() != null ? request.screenId() : show.getScreenId();
        UUID targetTheatreId = request.theatreId() != null ? request.theatreId() : show.getTheatreId();
        Instant targetStartTime = request.startTime() != null ? request.startTime() : show.getStartTime();

        if (request.movieId() != null) {
            validateMovieExists(targetMovieId);
        }
        if (request.screenId() != null) {
            validateScreenExists(targetScreenId);
        }
        if (request.startTime() != null) {
            validateStartTime(targetStartTime);
        }

        int durationMinutes = movieClient.getMovieDurationMinutes(targetMovieId);
        Instant targetEndTime = targetStartTime.plus(Duration.ofMinutes(durationMinutes + cleaningBufferMinutes));

        validateNoOverlap(targetScreenId, targetStartTime, targetEndTime, id);

        show.setMovieId(targetMovieId);
        show.setScreenId(targetScreenId);
        show.setTheatreId(targetTheatreId);
        show.setStartTime(targetStartTime);
        show.setEndTime(targetEndTime);

        if (request.language() != null && !request.language().isBlank()) {
            show.setLanguage(request.language().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            show.setStatus(request.status().trim());
        }

        log.info("Show updated successfully: id={}", id);
        return showMapper.toResponse(show);
    }

    // -------------------------------------------------------------------------
    // CANCEL
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public ShowResponse cancelShow(UUID id) {
        log.info("Cancelling show with id: {}", id);

        Show show = findActiveShowOrThrow(id);
        ensureNotCancelled(show);
        ensureNotStarted(show);

        show.setStatus("CANCELLED");

        log.info("Show cancelled successfully: id={}", id);
        return showMapper.toResponse(show);
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ShowResponse getShowById(UUID id) {
        log.debug("Fetching show by id: {}", id);
        Show show = findActiveShowOrThrow(id);
        return showMapper.toResponse(show);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovie(UUID movieId) {
        log.debug("Fetching shows for movieId: {}", movieId);
        validateMovieExists(movieId);
        return showMapper.toResponseList(showRepository.findByMovieIdAndDeletedAtIsNull(movieId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowResponse> getShowsByMovie(UUID movieId, Pageable pageable) {
        log.debug("Fetching page of shows for movieId: {}", movieId);
        validateMovieExists(movieId);
        return showRepository.findByMovieIdAndDeletedAtIsNull(movieId, pageable)
                .map(showMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByScreen(UUID screenId) {
        log.debug("Fetching shows for screenId: {}", screenId);
        validateScreenExists(screenId);
        return showMapper.toResponseList(showRepository.findByScreenIdAndDeletedAtIsNull(screenId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShowResponse> getShowsByScreen(UUID screenId, Pageable pageable) {
        log.debug("Fetching page of shows for screenId: {}", screenId);
        validateScreenExists(screenId);
        return showRepository.findByScreenIdAndDeletedAtIsNull(screenId, pageable)
                .map(showMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByDate(LocalDate date) {
        log.debug("Fetching shows for date: {}", date);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        List<Show> shows = showRepository.findByStartTimeBetweenAndDeletedAtIsNull(startOfDay, endOfDay);
        return showMapper.toResponseList(shows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByTheatreAndDate(UUID theatreId, LocalDate date) {
        log.debug("Fetching shows for theatreId: {} on date: {}", theatreId, date);
        Instant startOfDay = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant endOfDay = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);

        List<Show> shows = showRepository.findByTheatreIdAndStartTimeBetweenAndDeletedAtIsNull(theatreId, startOfDay, endOfDay);
        return showMapper.toResponseList(shows);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowSeatResponse> getShowSeats(UUID showId) {
        log.debug("Fetching seats for showId: {}", showId);
        findActiveShowOrThrow(showId);
        List<ShowSeat> seats = showSeatRepository.findByShowId(showId);
        return showSeatMapper.toResponseList(seats);
    }

    // -------------------------------------------------------------------------
    // Private Helpers & Validations
    // -------------------------------------------------------------------------

    private Show findActiveShowOrThrow(UUID id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ShowNotFoundException(id));
        if (show.getDeletedAt() != null) {
            throw new ShowNotFoundException(id);
        }
        return show;
    }

    private void validateMovieExists(UUID movieId) {
        if (!movieClient.existsMovie(movieId)) {
            throw new MovieNotFoundException(movieId);
        }
    }

    private void validateScreenExists(UUID screenId) {
        if (!screenClient.existsScreen(screenId)) {
            throw new ScreenNotFoundException(screenId);
        }
    }

    private void validateStartTime(Instant startTime) {
        if (startTime == null) {
            throw new InvalidShowTimeException("Show start time must not be null");
        }
        if (startTime.isBefore(Instant.now())) {
            throw new InvalidShowTimeException("Show start time must be in the future");
        }
    }

    private void validateNoOverlap(UUID screenId, Instant startTime, Instant endTime, UUID excludeShowId) {
        List<Show> conflicts = showRepository.findConflictingShows(screenId, startTime, endTime, excludeShowId);
        if (!conflicts.isEmpty()) {
            throw new ShowConflictException(screenId, startTime, endTime);
        }
    }

    private void ensureNotCancelled(Show show) {
        if ("CANCELLED".equalsIgnoreCase(show.getStatus())) {
            throw new ShowAlreadyCancelledException(show.getId());
        }
    }

    private void ensureNotStarted(Show show) {
        if (show.getStartTime().isBefore(Instant.now())) {
            throw new ShowAlreadyStartedException(show.getId());
        }
    }

    private BigDecimal resolvePrice(String category, BigDecimal defaultPrice, java.util.Map<String, BigDecimal> categoryPrices) {
        if (categoryPrices != null && category != null && categoryPrices.containsKey(category)) {
            return categoryPrices.get(category);
        }
        return defaultPrice != null ? defaultPrice : BigDecimal.ZERO;
    }
}
