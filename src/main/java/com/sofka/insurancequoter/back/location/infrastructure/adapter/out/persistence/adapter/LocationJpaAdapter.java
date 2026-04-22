package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

// JPA adapter that implements the LocationRepository output port
@Component
public class LocationJpaAdapter implements LocationRepository {

    private final LocationJpaRepository locationJpaRepository;
    private final LocationPersistenceMapper mapper;

    public LocationJpaAdapter(LocationJpaRepository locationJpaRepository,
                              LocationPersistenceMapper mapper) {
        this.locationJpaRepository = locationJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Location> findActiveByQuoteId(Long quoteId) {
        return locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Location> findAllByQuoteId(Long quoteId) {
        return locationJpaRepository.findByQuoteId(quoteId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void insertAll(Long quoteId, List<Location> locations) {
        if (locations.isEmpty()) return;
        List<LocationJpa> entities = locations.stream()
                .map(l -> mapper.toLocationJpa(quoteId, l))
                .toList();
        locationJpaRepository.saveAll(entities);
    }

    @Override
    public void deactivateByIndices(Long quoteId, List<Integer> indices) {
        if (indices.isEmpty()) return;
        List<LocationJpa> rows = locationJpaRepository.findByQuoteIdAndIndexIn(quoteId, indices);
        rows.forEach(jpa -> jpa.setActive(false));
        locationJpaRepository.saveAll(rows);
    }

    @Override
    public void reactivateByIndices(Long quoteId, List<Integer> indices) {
        if (indices.isEmpty()) return;
        List<LocationJpa> rows = locationJpaRepository.findByQuoteIdAndIndexIn(quoteId, indices);
        rows.forEach(jpa -> jpa.setActive(true));
        locationJpaRepository.saveAll(rows);
    }
}
