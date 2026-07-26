package com.krushna.moviebooking.show.service;

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
import com.krushna.moviebooking.show.service.impl.ShowServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShowServiceImplTest {

    @Mock
    private ShowRepository showRepository;

    @Mock
    private ShowSeatRepository showSeatRepository;

    @Mock
    private MovieClient movieClient;

    @Mock
    private ScreenClient screenClient;

    @Mock
    private ShowMapper showMapper;

    @Mock
    private ShowSeatMapper showSeatMapper;

    @InjectMocks
    private ShowServiceImpl showService;

    private UUID movieId;
    private UUID theatreId;
    private UUID screenId;
    private UUID showId;
    private Instant futureStartTime;

    @BeforeEach
    void setUp() {
        showService.setCleaningBufferMinutes(30);
        movieId = UUID.randomUUID();
        theatreId = UUID.randomUUID();
        screenId = UUID.randomUUID();
        showId = UUID.randomUUID();
        futureStartTime = Instant.now().plus(Duration.ofHours(2));
    }

    @Test
    @DisplayName("createShow successfully creates a show and initializes seats")
    void createShow_Success() {
        ShowRequest request = ShowRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(futureStartTime)
                .language("English")
                .defaultPrice(new BigDecimal("250.00"))
                .categoryPrices(Map.of("VIP", new BigDecimal("400.00")))
                .build();

        ScreenClient.SeatDto seat1 = new ScreenClient.SeatDto(UUID.randomUUID(), screenId, "A", 1, "REGULAR", true);
        ScreenClient.SeatDto seat2 = new ScreenClient.SeatDto(UUID.randomUUID(), screenId, "A", 2, "VIP", true);

        when(movieClient.existsMovie(movieId)).thenReturn(true);
        when(screenClient.existsScreen(screenId)).thenReturn(true);
        when(movieClient.getMovieDurationMinutes(movieId)).thenReturn(120);
        when(showRepository.findConflictingShows(eq(screenId), any(), any(), eq(null))).thenReturn(List.of());
        when(screenClient.getActiveSeatsByScreen(screenId)).thenReturn(List.of(seat1, seat2));

        Show savedShow = Show.builder()
                .id(showId)
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(futureStartTime)
                .endTime(futureStartTime.plus(Duration.ofMinutes(150)))
                .language("English")
                .status("SCHEDULED")
                .build();

        ShowResponse expectedResponse = ShowResponse.builder()
                .id(showId)
                .movieId(movieId)
                .status("SCHEDULED")
                .build();

        when(showRepository.save(any(Show.class))).thenReturn(savedShow);
        when(showMapper.toResponse(savedShow)).thenReturn(expectedResponse);

        ShowResponse response = showService.createShow(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(showId);
        verify(showRepository).save(any(Show.class));
    }

    @Test
    @DisplayName("createShow throws MovieNotFoundException when movie does not exist")
    void createShow_ThrowsMovieNotFoundException() {
        ShowRequest request = ShowRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(futureStartTime)
                .language("English")
                .defaultPrice(new BigDecimal("200.00"))
                .build();

        when(movieClient.existsMovie(movieId)).thenReturn(false);

        assertThatThrownBy(() -> showService.createShow(request))
                .isInstanceOf(MovieNotFoundException.class);
    }

    @Test
    @DisplayName("createShow throws ScreenNotFoundException when screen does not exist")
    void createShow_ThrowsScreenNotFoundException() {
        ShowRequest request = ShowRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(futureStartTime)
                .language("English")
                .defaultPrice(new BigDecimal("200.00"))
                .build();

        when(movieClient.existsMovie(movieId)).thenReturn(true);
        when(screenClient.existsScreen(screenId)).thenReturn(false);

        assertThatThrownBy(() -> showService.createShow(request))
                .isInstanceOf(ScreenNotFoundException.class);
    }

    @Test
    @DisplayName("createShow throws InvalidShowTimeException when start time is in the past")
    void createShow_ThrowsInvalidShowTimeException_PastTime() {
        ShowRequest request = ShowRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(Instant.now().minus(Duration.ofMinutes(10)))
                .language("English")
                .defaultPrice(new BigDecimal("200.00"))
                .build();

        when(movieClient.existsMovie(movieId)).thenReturn(true);
        when(screenClient.existsScreen(screenId)).thenReturn(true);

        assertThatThrownBy(() -> showService.createShow(request))
                .isInstanceOf(InvalidShowTimeException.class);
    }

    @Test
    @DisplayName("createShow throws ShowConflictException when show overlaps")
    void createShow_ThrowsShowConflictException() {
        ShowRequest request = ShowRequest.builder()
                .movieId(movieId)
                .theatreId(theatreId)
                .screenId(screenId)
                .startTime(futureStartTime)
                .language("English")
                .defaultPrice(new BigDecimal("200.00"))
                .build();

        when(movieClient.existsMovie(movieId)).thenReturn(true);
        when(screenClient.existsScreen(screenId)).thenReturn(true);
        when(movieClient.getMovieDurationMinutes(movieId)).thenReturn(120);

        Show existingShow = Show.builder().id(UUID.randomUUID()).build();
        when(showRepository.findConflictingShows(eq(screenId), any(), any(), eq(null)))
                .thenReturn(List.of(existingShow));

        assertThatThrownBy(() -> showService.createShow(request))
                .isInstanceOf(ShowConflictException.class);
    }

    @Test
    @DisplayName("cancelShow successfully cancels a scheduled show")
    void cancelShow_Success() {
        Show show = Show.builder()
                .id(showId)
                .startTime(futureStartTime)
                .status("SCHEDULED")
                .build();

        when(showRepository.findById(showId)).thenReturn(Optional.of(show));
        when(showMapper.toResponse(show)).thenReturn(ShowResponse.builder().id(showId).status("CANCELLED").build());

        ShowResponse response = showService.cancelShow(showId);

        assertThat(response.status()).isEqualTo("CANCELLED");
        assertThat(show.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("cancelShow throws ShowAlreadyCancelledException if already cancelled")
    void cancelShow_AlreadyCancelled() {
        Show show = Show.builder()
                .id(showId)
                .startTime(futureStartTime)
                .status("CANCELLED")
                .build();

        when(showRepository.findById(showId)).thenReturn(Optional.of(show));

        assertThatThrownBy(() -> showService.cancelShow(showId))
                .isInstanceOf(ShowAlreadyCancelledException.class);
    }

    @Test
    @DisplayName("cancelShow throws ShowAlreadyStartedException if show has started")
    void cancelShow_AlreadyStarted() {
        Show show = Show.builder()
                .id(showId)
                .startTime(Instant.now().minus(Duration.ofMinutes(30)))
                .status("SCHEDULED")
                .build();

        when(showRepository.findById(showId)).thenReturn(Optional.of(show));

        assertThatThrownBy(() -> showService.cancelShow(showId))
                .isInstanceOf(ShowAlreadyStartedException.class);
    }

    @Test
    @DisplayName("getShowsByDate returns shows within the requested day")
    void getShowsByDate_Success() {
        LocalDate today = LocalDate.now();
        Show show = Show.builder().id(showId).startTime(futureStartTime).build();
        when(showRepository.findByStartTimeBetweenAndDeletedAtIsNull(any(), any())).thenReturn(List.of(show));
        when(showMapper.toResponseList(List.of(show))).thenReturn(List.of(ShowResponse.builder().id(showId).build()));

        List<ShowResponse> shows = showService.getShowsByDate(today);

        assertThat(shows).hasSize(1);
    }
}
