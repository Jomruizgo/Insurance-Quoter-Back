package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.in.GetCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;

import java.util.List;

// Orchestrates: assert folio exists → fetch options → fetch version → return response
public class GetCoverageOptionsUseCaseImpl implements GetCoverageOptionsUseCase {

    private final QuoteLookupPort quoteLookupPort;
    private final CoverageOptionRepository coverageOptionRepository;

    public GetCoverageOptionsUseCaseImpl(QuoteLookupPort quoteLookupPort,
                                         CoverageOptionRepository coverageOptionRepository) {
        this.quoteLookupPort = quoteLookupPort;
        this.coverageOptionRepository = coverageOptionRepository;
    }

    @Override
    public CoverageOptionsResponse getCoverageOptions(String folioNumber) {
        quoteLookupPort.assertFolioExists(folioNumber);
        List<CoverageOption> options = coverageOptionRepository.findByFolioNumber(folioNumber);
        long version = quoteLookupPort.getCurrentVersion(folioNumber);
        java.time.Instant updatedAt = quoteLookupPort.getUpdatedAt(folioNumber);
        return new CoverageOptionsResponse(folioNumber, options, updatedAt, version);
    }
}
