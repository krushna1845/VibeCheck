package com.krushna.moviebooking.theatre.service;

import com.krushna.moviebooking.theatre.dto.TheatreRequest;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;

import com.krushna.moviebooking.theatre.entity.City;
import com.krushna.moviebooking.theatre.entity.Theatre;
import com.krushna.moviebooking.theatre.exception.CityNotFoundException;
import com.krushna.moviebooking.theatre.exception.DuplicateTheatreException;
import com.krushna.moviebooking.theatre.exception.TheatreNotFoundException;
import com.krushna.moviebooking.theatre.mapper.TheatreMapper;
import com.krushna.moviebooking.theatre.repository.CityRepository;
import com.krushna.moviebooking.theatre.repository.TheatreRepository;
import com.krushna.moviebooking.theatre.service.impl.TheatreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TheatreServiceImplTest {

    @Mock
    private TheatreRepository theatreRepository;

    @Mock
    private CityRepository cityRepository;

    @Mock
    private TheatreMapper theatreMapper;

    @InjectMocks
    private TheatreServiceImpl theatreService;

    private UUID theatreId;
    private City city;
    private Theatre theatre;
    private TheatreResponse theatreResponse;

    @BeforeEach
    void setUp() {
        theatreId = UUID.randomUUID();
        city = City.builder().id(1).name("Mumbai").state("Maharashtra").pincode("400001").build();
        theatre = Theatre.builder()
                .id(theatreId)
                .city(city)
                .name("PVR ICON")
                .address("Oberoi Mall, Goregaon")
                .status("ACTIVE")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        theatreResponse = new TheatreResponse(
                theatreId,
                new TheatreResponse.CitySummary(1, "Mumbai", "Maharashtra", "India", "400001"),
                "PVR ICON", "Oberoi Mall, Goregaon", null, null, "ACTIVE",
                Instant.now(), Instant.now(), null
        );
    }

    @Test
    void createTheatre_Success() {
        TheatreRequest request = TheatreRequest.builder()
                .cityId(1)
                .name("PVR ICON")
                .address("Oberoi Mall, Goregaon")
                .build();

        when(cityRepository.findById(1)).thenReturn(Optional.of(city));
        when(theatreRepository.existsByCityIdAndNameIgnoreCaseAndDeletedAtIsNull(1, "PVR ICON")).thenReturn(false);
        when(theatreRepository.save(any(Theatre.class))).thenReturn(theatre);
        when(theatreMapper.toResponse(any(Theatre.class))).thenReturn(theatreResponse);

        TheatreResponse result = theatreService.createTheatre(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("PVR ICON");
        verify(theatreRepository).save(any(Theatre.class));
    }

    @Test
    void createTheatre_CityNotFound_ThrowsException() {
        TheatreRequest request = TheatreRequest.builder()
                .cityId(99)
                .name("PVR ICON")
                .address("Address")
                .build();

        when(cityRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> theatreService.createTheatre(request))
                .isInstanceOf(CityNotFoundException.class);
    }

    @Test
    void createTheatre_DuplicateNameInCity_ThrowsException() {
        TheatreRequest request = TheatreRequest.builder()
                .cityId(1)
                .name("PVR ICON")
                .address("Address")
                .build();

        when(cityRepository.findById(1)).thenReturn(Optional.of(city));
        when(theatreRepository.existsByCityIdAndNameIgnoreCaseAndDeletedAtIsNull(1, "PVR ICON")).thenReturn(true);

        assertThatThrownBy(() -> theatreService.createTheatre(request))
                .isInstanceOf(DuplicateTheatreException.class);
    }

    @Test
    void getTheatreById_Success() {
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.of(theatre));
        when(theatreMapper.toResponse(theatre)).thenReturn(theatreResponse);

        TheatreResponse result = theatreService.getTheatreById(theatreId);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(theatreId);
    }

    @Test
    void getTheatreById_NotFound_ThrowsException() {
        when(theatreRepository.findById(theatreId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> theatreService.getTheatreById(theatreId))
                .isInstanceOf(TheatreNotFoundException.class);
    }
}
