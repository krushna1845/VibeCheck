package com.krushna.moviebooking.theatre.service.impl;

import com.krushna.moviebooking.theatre.dto.TheatreRequest;
import com.krushna.moviebooking.theatre.dto.TheatreResponse;
import com.krushna.moviebooking.theatre.dto.TheatreUpdateRequest;
import com.krushna.moviebooking.theatre.entity.City;
import com.krushna.moviebooking.theatre.entity.Theatre;
import com.krushna.moviebooking.theatre.exception.CityNotFoundException;
import com.krushna.moviebooking.theatre.exception.DuplicateTheatreException;
import com.krushna.moviebooking.theatre.exception.InactiveTheatreException;
import com.krushna.moviebooking.theatre.exception.TheatreNotFoundException;
import com.krushna.moviebooking.theatre.mapper.TheatreMapper;
import com.krushna.moviebooking.theatre.repository.CityRepository;
import com.krushna.moviebooking.theatre.repository.TheatreRepository;
import com.krushna.moviebooking.theatre.service.TheatreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Primary implementation of {@link TheatreService}.
 *
 * Enforces business rules:
 * - Theatre names must be unique within the same city.
 * - Soft delete only (sets deletedAt timestamp and status to DELETED).
 * - Read methods use @Transactional(readOnly = true).
 * - Write methods use @Transactional.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TheatreServiceImpl implements TheatreService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("ACTIVE", "INACTIVE", "MAINTENANCE", "CLOSED", "DELETED");

    private final TheatreRepository theatreRepository;
    private final CityRepository cityRepository;
    private final TheatreMapper theatreMapper;

    @Override
    @Transactional
    public TheatreResponse createTheatre(TheatreRequest request) {
        log.info("Creating theatre '{}' in city id: {}", request.name(), request.cityId());

        City city = cityRepository.findById(request.cityId())
                .orElseThrow(() -> new CityNotFoundException(request.cityId()));

        validateUniqueTheatreNameInCity(request.cityId(), request.name());

        Theatre theatre = Theatre.builder()
                .city(city)
                .name(request.name().trim())
                .address(request.address().trim())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .status("ACTIVE")
                .build();

        Theatre saved = theatreRepository.save(theatre);
        log.info("Theatre created successfully with id: {}", saved.getId());
        return theatreMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public TheatreResponse updateTheatre(UUID id, TheatreUpdateRequest request) {
        log.info("Updating theatre with id: {}", id);

        Theatre theatre = findActiveTheatreOrThrow(id);

        if (request.cityId() != null && !theatre.getCity().getId().equals(request.cityId())) {
            City newCity = cityRepository.findById(request.cityId())
                    .orElseThrow(() -> new CityNotFoundException(request.cityId()));
            theatre.setCity(newCity);
        }

        if (StringUtils.hasText(request.name())) {
            String newName = request.name().trim();
            if (!theatre.getName().equalsIgnoreCase(newName)) {
                validateUniqueTheatreNameInCity(theatre.getCity().getId(), newName);
                theatre.setName(newName);
            }
        }

        if (StringUtils.hasText(request.address())) {
            theatre.setAddress(request.address().trim());
        }
        if (request.latitude() != null) {
            theatre.setLatitude(request.latitude());
        }
        if (request.longitude() != null) {
            theatre.setLongitude(request.longitude());
        }
        if (StringUtils.hasText(request.status())) {
            validateStatus(request.status());
            theatre.setStatus(request.status().trim().toUpperCase());
        }

        log.info("Theatre updated successfully: id={}", id);
        return theatreMapper.toResponse(theatre);
    }

    @Override
    @Transactional
    public void deleteTheatre(UUID id) {
        log.info("Soft-deleting theatre with id: {}", id);

        Theatre theatre = findActiveTheatreOrThrow(id);
        theatre.setDeletedAt(Instant.now());
        theatre.setStatus("DELETED");

        log.info("Theatre soft-deleted successfully: id={}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public TheatreResponse getTheatreById(UUID id) {
        log.debug("Fetching active theatre by id: {}", id);

        Theatre theatre = findActiveTheatreOrThrow(id);
        return theatreMapper.toResponse(theatre);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TheatreResponse> getAllTheatres(Pageable pageable) {
        log.debug("Fetching all active theatres page: {}", pageable.getPageNumber());

        return theatreRepository.findByDeletedAtIsNull(pageable)
                .map(theatreMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TheatreResponse> getTheatresByCity(Integer cityId) {
        log.debug("Fetching theatres for city id: {}", cityId);

        if (!cityRepository.existsById(cityId)) {
            throw new CityNotFoundException(cityId);
        }

        return theatreRepository.findByCityIdAndDeletedAtIsNull(cityId).stream()
                .map(theatreMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TheatreResponse> searchTheatres(String keyword, Pageable pageable) {
        log.debug("Searching theatres by keyword: '{}'", keyword);

        return theatreRepository.findByNameContainingIgnoreCaseAndDeletedAtIsNull(keyword, pageable)
                .map(theatreMapper::toResponse);
    }

    @Override
    @Transactional
    public TheatreResponse changeTheatreStatus(UUID id, String status) {
        log.info("Changing status of theatre id={} to '{}'", id, status);

        validateStatus(status);
        Theatre theatre = findActiveTheatreOrThrow(id);

        theatre.setStatus(status.trim().toUpperCase());
        log.info("Theatre id={} status updated to '{}'", id, status);
        return theatreMapper.toResponse(theatre);
    }

    private void validateUniqueTheatreNameInCity(Integer cityId, String name) {
        if (theatreRepository.existsByCityIdAndNameIgnoreCaseAndDeletedAtIsNull(cityId, name.trim())) {
            throw new DuplicateTheatreException(name, cityId);
        }
    }

    private Theatre findActiveTheatreOrThrow(UUID id) {
        Theatre theatre = theatreRepository.findById(id)
                .orElseThrow(() -> new TheatreNotFoundException(id));

        if (theatre.getDeletedAt() != null) {
            throw new InactiveTheatreException("Theatre with id " + id + " has been soft-deleted.");
        }
        return theatre;
    }

    private void validateStatus(String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status.trim().toUpperCase())) {
            throw new IllegalArgumentException("Invalid status: '" + status + "'. Allowed values: " + ALLOWED_STATUSES);
        }
    }
}
