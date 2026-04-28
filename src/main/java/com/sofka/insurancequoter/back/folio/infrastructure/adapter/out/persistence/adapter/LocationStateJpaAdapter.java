package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.domain.model.LocationStateSummary;
import com.sofka.insurancequoter.back.folio.domain.port.out.LocationStateReader;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LocationStateJpaAdapter implements LocationStateReader {

    private final QuoteJpaRepository quoteJpaRepository;
    private final LocationJpaRepository locationJpaRepository;

    @Override
    public LocationStateSummary readByFolioNumber(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .map(quote -> {
                    List<LocationJpa> locations = locationJpaRepository.findByQuoteIdAndActiveTrue(quote.getId());
                    long completeCount = locations.stream()
                            .filter(l -> "COMPLETE".equals(l.getValidationStatus()))
                            .count();
                    long incompleteCount = locations.stream()
                            .filter(l -> l.getBlockingAlerts() != null && !l.getBlockingAlerts().isEmpty())
                            .count();
                    return new LocationStateSummary(locations.size(), completeCount, incompleteCount);
                })
                .orElse(new LocationStateSummary(0, 0, 0));
    }
}
