package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import java.util.Optional;

// JPA adapter that implements the QuoteLayoutRepository output port
@Component
public class QuoteLayoutJpaAdapter implements QuoteLayoutRepository {

    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationPersistenceMapper mapper;

    public QuoteLayoutJpaAdapter(QuoteJpaRepository quoteJpaRepository,
                                 LocationPersistenceMapper mapper) {
        this.quoteJpaRepository = quoteJpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<QuoteLayoutData> findByFolioNumber(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .map(mapper::toQuoteLayoutData);
    }

    @Override
    public QuoteLayoutData save(QuoteLayoutData data) {
        // Load the current managed entity from the database
        QuoteJpa jpa = quoteJpaRepository.findById(data.id())
                .orElseThrow(() -> new IllegalStateException(
                        "Quote not found by id during save: " + data.id()));

        // Manual optimistic lock check: compare client-supplied version with the current DB version.
        // Hibernate manages @Version internally on managed entities and ignores setVersion(),
        // so we perform the guard here before any mutation.
        if (!jpa.getVersion().equals(data.version())) {
            throw new ObjectOptimisticLockingFailureException(QuoteJpa.class, data.id());
        }

        jpa.setNumberOfLocations(data.numberOfLocations());
        jpa.setLocationType(data.locationType());

        QuoteJpa saved = quoteJpaRepository.saveAndFlush(jpa);
        return mapper.toQuoteLayoutData(saved);
    }
}
