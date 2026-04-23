package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.LocationSummary;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// JPA adapter that implements the LocationRepository output port
@Component
public class LocationJpaAdapter implements LocationRepository {

    private final LocationJpaRepository locationJpaRepository;
    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationPersistenceMapper mapper;

    public LocationJpaAdapter(LocationJpaRepository locationJpaRepository,
                              QuoteJpaRepository quoteJpaRepository,
                              LocationPersistenceMapper mapper) {
        this.locationJpaRepository = locationJpaRepository;
        this.quoteJpaRepository = quoteJpaRepository;
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

    @Override
    public List<Location> findByFolioNumber(String folioNumber) {
        QuoteJpa quote = requireQuote(folioNumber);
        return locationJpaRepository.findByQuoteId(quote.getId())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<Location> replaceAll(String folioNumber, List<Location> locations) {
        QuoteJpa quote = requireQuote(folioNumber);
        locationJpaRepository.deleteByQuoteId(quote.getId());
        locationJpaRepository.flush();

        List<LocationJpa> entities = locations.stream()
                .map(l -> mapper.toLocationJpa(quote.getId(), l))
                .toList();
        List<LocationJpa> saved = locationJpaRepository.saveAll(entities);
        return saved.stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public Location patchOne(String folioNumber, int index, Location mergedLocation) {
        QuoteJpa quote = requireQuote(folioNumber);
        LocationJpa existing = locationJpaRepository
                .findByQuoteIdAndIndex(quote.getId(), index)
                .orElseThrow(() -> new com.sofka.insurancequoter.back.location.application.usecase
                        .LocationNotFoundException(folioNumber, index));

        // Update mutable fields in-place to preserve @CreationTimestamp and id
        existing.setLocationName(mergedLocation.locationName());
        existing.setAddress(mergedLocation.address());
        existing.setZipCode(mergedLocation.zipCode());
        existing.setState(mergedLocation.state());
        existing.setMunicipality(mergedLocation.municipality());
        existing.setNeighborhood(mergedLocation.neighborhood());
        existing.setCity(mergedLocation.city());
        existing.setConstructionType(mergedLocation.constructionType());
        existing.setLevel(mergedLocation.level());
        existing.setConstructionYear(mergedLocation.constructionYear());
        existing.setBusinessLineCode(mergedLocation.businessLine() != null ? mergedLocation.businessLine().code() : null);
        existing.setBusinessLineFireKey(mergedLocation.businessLine() != null ? mergedLocation.businessLine().fireKey() : null);
        existing.setBusinessLineDescription(mergedLocation.businessLine() != null ? mergedLocation.businessLine().description() : null);
        existing.setGuarantees(mergedLocation.guarantees());
        existing.setCatastrophicZone(mergedLocation.catastrophicZone());
        existing.setValidationStatus(mergedLocation.validationStatus() != null
                ? mergedLocation.validationStatus().name()
                : "INCOMPLETE");

        // Replace blocking alerts collection
        existing.getBlockingAlerts().clear();
        if (mergedLocation.blockingAlerts() != null) {
            mergedLocation.blockingAlerts().forEach(a ->
                    existing.getBlockingAlerts().add(
                            new com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities
                                    .BlockingAlertEmbeddable(a.code(), a.message())));
        }

        LocationJpa saved = locationJpaRepository.save(existing);
        return mapper.toDomain(saved);
    }

    @Override
    public List<LocationSummary> findSummaryByFolioNumber(String folioNumber) {
        QuoteJpa quote = requireQuote(folioNumber);
        return locationJpaRepository.findByQuoteId(quote.getId())
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    public boolean existsByFolioAndIndex(String folioNumber, int index) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .map(q -> locationJpaRepository.existsByQuoteIdAndIndex(q.getId(), index))
                .orElse(false);
    }

    private QuoteJpa requireQuote(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
    }
}
