package com.krushna.moviebooking.theatre.service.impl;

import com.krushna.moviebooking.theatre.dto.CityRequest;
import com.krushna.moviebooking.theatre.dto.CityResponse;
import com.krushna.moviebooking.theatre.entity.City;
import com.krushna.moviebooking.theatre.exception.CityNotFoundException;
import com.krushna.moviebooking.theatre.mapper.CityMapper;
import com.krushna.moviebooking.theatre.repository.CityRepository;
import com.krushna.moviebooking.theatre.service.CityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Implementation of {@link CityService}.
 * Enforces transaction boundaries, validation, and structured logging.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepository cityRepository;
    private final CityMapper cityMapper;

    @Override
    @Transactional
    public CityResponse createCity(CityRequest request) {
        log.info("Creating city with name '{}' in state '{}'", request.name(), request.state());

        validateUniqueCity(request.name(), request.state());

        City city = cityMapper.toEntity(request);
        if (!StringUtils.hasText(city.getCountry())) {
            city.setCountry("India");
        }

        City saved = cityRepository.save(city);
        log.info("City created successfully with id: {}", saved.getId());
        return cityMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CityResponse updateCity(Integer id, CityRequest request) {
        log.info("Updating city id: {}", id);

        City city = cityRepository.findById(id)
                .orElseThrow(() -> new CityNotFoundException(id));

        if (StringUtils.hasText(request.name()) && StringUtils.hasText(request.state())) {
            if (!city.getName().equalsIgnoreCase(request.name())
                    || !city.getState().equalsIgnoreCase(request.state())) {
                validateUniqueCity(request.name(), request.state());
            }
        }

        if (StringUtils.hasText(request.name())) {
            city.setName(request.name().trim());
        }
        if (StringUtils.hasText(request.state())) {
            city.setState(request.state().trim());
        }
        if (request.country() != null) {
            city.setCountry(request.country().trim());
        }
        if (request.pincode() != null) {
            city.setPincode(request.pincode().trim());
        }

        log.info("City updated successfully: id={}", id);
        return cityMapper.toResponse(city);
    }

    @Override
    @Transactional(readOnly = true)
    public CityResponse getCityById(Integer id) {
        log.debug("Fetching city by id: {}", id);

        City city = cityRepository.findById(id)
                .orElseThrow(() -> new CityNotFoundException(id));

        return cityMapper.toResponse(city);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> getAllCities() {
        log.debug("Fetching all cities");

        return cityRepository.findAll().stream()
                .map(cityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CityResponse> searchCities(String name) {
        log.debug("Searching cities with name keyword: '{}'", name);

        return cityRepository.findByNameContainingIgnoreCase(name).stream()
                .map(cityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void deleteCity(Integer id) {
        log.info("Deleting city with id: {}", id);

        City city = cityRepository.findById(id)
                .orElseThrow(() -> new CityNotFoundException(id));

        cityRepository.delete(city);
        log.info("City deleted successfully: id={}", id);
    }

    private void validateUniqueCity(String name, String state) {
        if (cityRepository.existsByNameIgnoreCaseAndStateIgnoreCase(name.trim(), state.trim())) {
            throw new IllegalArgumentException("A city with name '" + name + "' in state '" + state + "' already exists.");
        }
    }
}
