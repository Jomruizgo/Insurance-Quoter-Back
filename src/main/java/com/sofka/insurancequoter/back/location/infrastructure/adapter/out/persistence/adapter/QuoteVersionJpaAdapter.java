package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// Reads and increments the optimistic lock version on the quotes table
@Component
public class QuoteVersionJpaAdapter implements QuoteVersionRepository {

    private final QuoteJpaRepository quoteJpaRepository;

    public QuoteVersionJpaAdapter(QuoteJpaRepository quoteJpaRepository) {
        this.quoteJpaRepository = quoteJpaRepository;
    }

    @Override
    public Long findVersionByFolioNumber(String folioNumber) {
        return requireQuote(folioNumber).getVersion();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementVersion(String folioNumber) {
        quoteJpaRepository.incrementVersionByFolioNumber(folioNumber, Instant.now());
    }

    @Override
    public Instant getUpdatedAt(String folioNumber) {
        return requireQuote(folioNumber).getUpdatedAt();
    }

    private QuoteJpa requireQuote(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
    }
}
