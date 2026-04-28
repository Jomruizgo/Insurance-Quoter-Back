package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// Persistence adapter: implements FolioListQuery using QuoteJpaRepository.findAll().
// Maps QuoteJpa -> FolioRaw; agentName and completionPct are NOT resolved here.
// locationCount is resolved from the locations table via LocationJpaRepository.
@Component
@RequiredArgsConstructor
public class FolioListJpaAdapter implements FolioListQuery {

    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationJpaRepository locationJpaRepository;

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
                locationJpaRepository.countByQuoteId(jpa.getId()),
                jpa.getUpdatedAt()
        );
    }
}
