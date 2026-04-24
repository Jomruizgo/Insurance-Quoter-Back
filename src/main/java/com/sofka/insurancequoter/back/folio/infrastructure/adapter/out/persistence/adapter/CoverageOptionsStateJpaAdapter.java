package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.domain.model.SectionStatus;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoverageOptionsStateReader;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CoverageOptionsStateJpaAdapter implements CoverageOptionsStateReader {

    private final QuoteJpaRepository quoteJpaRepository;
    private final CoverageOptionJpaRepository coverageOptionJpaRepository;

    @Override
    public SectionStatus readByFolioNumber(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .map(quote -> {
                    var options = coverageOptionJpaRepository.findAllByQuote_Id(quote.getId());
                    if (options.isEmpty()) return SectionStatus.PENDING;
                    boolean anySelected = options.stream().anyMatch(o -> o.isSelected());
                    return anySelected ? SectionStatus.COMPLETE : SectionStatus.IN_PROGRESS;
                })
                .orElse(SectionStatus.PENDING);
    }
}
