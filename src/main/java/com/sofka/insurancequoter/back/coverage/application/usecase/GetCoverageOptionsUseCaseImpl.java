package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.in.GetCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.out.ActiveGuaranteeReader;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;

import java.time.Instant;
import java.util.List;

// Orchestrates: assert folio exists → fetch options from DB → if empty, derive from guarantees → return response
public class GetCoverageOptionsUseCaseImpl implements GetCoverageOptionsUseCase {

    private final QuoteLookupPort quoteLookupPort;
    private final CoverageOptionRepository coverageOptionRepository;
    private final ActiveGuaranteeReader activeGuaranteeReader;
    private final CoverageDerivationService coverageDerivationService;

    public GetCoverageOptionsUseCaseImpl(QuoteLookupPort quoteLookupPort,
                                         CoverageOptionRepository coverageOptionRepository,
                                         ActiveGuaranteeReader activeGuaranteeReader,
                                         CoverageDerivationService coverageDerivationService) {
        this.quoteLookupPort = quoteLookupPort;
        this.coverageOptionRepository = coverageOptionRepository;
        this.activeGuaranteeReader = activeGuaranteeReader;
        this.coverageDerivationService = coverageDerivationService;
    }

    @Override
    @io.micrometer.observation.annotation.Observed(name = "coverage.options.get")
    public CoverageOptionsResponse getCoverageOptions(String folioNumber) {
        quoteLookupPort.assertFolioExists(folioNumber);

        List<CoverageOption> options = coverageOptionRepository.findByFolioNumber(folioNumber);

        // If no options are persisted, derive them from the location guarantees
        if (options.isEmpty()) {
            List<String> activeCodes = activeGuaranteeReader.readActiveGuaranteeCodes(folioNumber);
            boolean hasCatZone = activeGuaranteeReader.hasCatastrophicZone(folioNumber);
            options = coverageDerivationService.deriveFrom(activeCodes, hasCatZone);
        }

        long version = quoteLookupPort.getCurrentVersion(folioNumber);
        Instant updatedAt = quoteLookupPort.getUpdatedAt(folioNumber);
        return new CoverageOptionsResponse(folioNumber, options, updatedAt, version);
    }
}
