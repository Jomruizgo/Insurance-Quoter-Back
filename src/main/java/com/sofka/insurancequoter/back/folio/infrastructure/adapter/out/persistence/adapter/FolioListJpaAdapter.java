package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// Persistence adapter: implements FolioListQuery using QuoteJpaRepository.findAll().
// Maps QuoteJpa -> FolioRaw; agentName and completionPct are NOT resolved here.
@Component
@RequiredArgsConstructor
public class FolioListJpaAdapter implements FolioListQuery {

    private final QuoteJpaRepository quoteJpaRepository;

    @Override
    public List<FolioRaw> findAll() {
        return quoteJpaRepository.findAll().stream()
                .map(this::toRaw)
                .toList();
    }

    private FolioRaw toRaw(QuoteJpa jpa) {
        return new FolioRaw(
                jpa.getFolioNumber(),
                jpa.getInsuredName(),
                jpa.getAgentCode(),
                jpa.getQuoteStatus(),
                jpa.getNumberOfLocations() != null ? jpa.getNumberOfLocations() : 0,
                jpa.getUpdatedAt()
        );
    }
}
