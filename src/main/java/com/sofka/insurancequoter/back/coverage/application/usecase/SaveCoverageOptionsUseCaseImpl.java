package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.in.SaveCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Orchestrates: assert folio → check version → validate COV-* codes → enrich descriptions → persist → return
public class SaveCoverageOptionsUseCaseImpl implements SaveCoverageOptionsUseCase {

    private final QuoteLookupPort quoteLookupPort;
    private final CoverageOptionRepository coverageOptionRepository;
    private final CoverageDerivationService coverageDerivationService;
    private final Counter savedCounter;

    public SaveCoverageOptionsUseCaseImpl(QuoteLookupPort quoteLookupPort,
                                          CoverageOptionRepository coverageOptionRepository,
                                          CoverageDerivationService coverageDerivationService,
                                          MeterRegistry meterRegistry) {
        this.quoteLookupPort = quoteLookupPort;
        this.coverageOptionRepository = coverageOptionRepository;
        this.coverageDerivationService = coverageDerivationService;
        this.savedCounter = Counter.builder("coverage.options.saved")
                .description("Coverage option sets saved")
                .register(meterRegistry);
    }

    @Override
    @Observed(name = "coverage.options.save")
    public CoverageOptionsResponse saveCoverageOptions(SaveCoverageOptionsCommand command) {
        quoteLookupPort.assertFolioExists(command.folioNumber());
        quoteLookupPort.assertVersionMatches(command.folioNumber(), command.version());

        Set<String> seenCodes = new HashSet<>();
        for (CoverageOption option : command.coverageOptions()) {
            if (!seenCodes.add(option.code())) {
                throw new InvalidCoverageCodeException(option.code());
            }
        }

        Map<String, String> knownDescriptions = coverageDerivationService.knownDescriptions();

        List<CoverageOption> enriched = command.coverageOptions().stream()
                .map(option -> {
                    if (!knownDescriptions.containsKey(option.code())) {
                        throw new InvalidCoverageCodeException(option.code());
                    }
                    return new CoverageOption(
                            option.code(),
                            knownDescriptions.get(option.code()),
                            option.selected(),
                            option.deductiblePercentage(),
                            option.coinsurancePercentage()
                    );
                })
                .toList();

        List<CoverageOption> saved = coverageOptionRepository.replaceAll(command.folioNumber(), enriched);
        long newVersion = quoteLookupPort.getCurrentVersion(command.folioNumber());
        java.time.Instant updatedAt = quoteLookupPort.getUpdatedAt(command.folioNumber());
        savedCounter.increment();
        return new CoverageOptionsResponse(command.folioNumber(), saved, updatedAt, newVersion);
    }
}
