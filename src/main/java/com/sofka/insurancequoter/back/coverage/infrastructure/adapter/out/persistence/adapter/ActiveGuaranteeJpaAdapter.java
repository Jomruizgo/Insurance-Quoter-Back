package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.coverage.domain.port.out.ActiveGuaranteeReader;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// Implements ActiveGuaranteeReader using JPA repositories.
// Reads location guarantees persisted as JSON (converted by GuaranteesConverter) for a given folio.
public class ActiveGuaranteeJpaAdapter implements ActiveGuaranteeReader {

    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationJpaRepository locationJpaRepository;

    public ActiveGuaranteeJpaAdapter(QuoteJpaRepository quoteJpaRepository,
                                      LocationJpaRepository locationJpaRepository) {
        this.quoteJpaRepository = quoteJpaRepository;
        this.locationJpaRepository = locationJpaRepository;
    }

    @Override
    public List<String> readActiveGuaranteeCodes(String folioNumber) {
        Long quoteId = resolveQuoteId(folioNumber);
        List<LocationJpa> locations = locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId);

        return locations.stream()
                .map(LocationJpa::getGuarantees)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(this::isActive)
                .map(Guarantee::code)
                .distinct()
                .toList();
    }

    @Override
    public boolean hasCatastrophicZone(String folioNumber) {
        Long quoteId = resolveQuoteId(folioNumber);
        List<LocationJpa> locations = locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId);

        return locations.stream()
                .map(LocationJpa::getCatastrophicZone)
                .anyMatch(zone -> zone != null && !zone.isBlank());
    }

    // A guarantee is active when its insuredValue is non-null and greater than zero
    private boolean isActive(Guarantee guarantee) {
        return guarantee.insuredValue() != null
                && guarantee.insuredValue().compareTo(BigDecimal.ZERO) > 0;
    }

    private Long resolveQuoteId(String folioNumber) {
        QuoteJpa quote = quoteJpaRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
        return quote.getId();
    }
}
