package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.domain.model.QuoteSnapshot;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteStateQuery;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QuoteStateJpaAdapter implements QuoteStateQuery {

    private final QuoteJpaRepository quoteJpaRepository;

    @Override
    public QuoteSnapshot findByFolioNumber(String folioNumber) {
        QuoteJpa jpa = quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
        return new QuoteSnapshot(
                jpa.getFolioNumber(),
                jpa.getQuoteStatus(),
                jpa.getNumberOfLocations(),
                jpa.getLocationType(),
                jpa.getVersion(),
                jpa.getUpdatedAt(),
                jpa.getInsuredName() != null
        );
    }
}
