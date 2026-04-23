package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.in.SaveCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// Orchestrates: assert folio → check version → validate codes → enrich descriptions → persist → return
public class SaveCoverageOptionsUseCaseImpl implements SaveCoverageOptionsUseCase {

    private final QuoteLookupPort quoteLookupPort;
    private final CoverageOptionRepository coverageOptionRepository;
    private final GuaranteeCatalogClient guaranteeCatalogClient;

    public SaveCoverageOptionsUseCaseImpl(QuoteLookupPort quoteLookupPort,
                                          CoverageOptionRepository coverageOptionRepository,
                                          GuaranteeCatalogClient guaranteeCatalogClient) {
        this.quoteLookupPort = quoteLookupPort;
        this.coverageOptionRepository = coverageOptionRepository;
        this.guaranteeCatalogClient = guaranteeCatalogClient;
    }

    @Override
    public CoverageOptionsResponse saveCoverageOptions(SaveCoverageOptionsCommand command) {
        quoteLookupPort.assertFolioExists(command.folioNumber());
        quoteLookupPort.assertVersionMatches(command.folioNumber(), command.version());

        Set<String> seenCodes = new HashSet<>();
        for (CoverageOption option : command.coverageOptions()) {
            if (!seenCodes.add(option.code())) {
                throw new InvalidCoverageCodeException(option.code());
            }
        }

        List<GuaranteeDto> catalog = guaranteeCatalogClient.fetchGuarantees();
        Map<String, String> catalogByCode = catalog.stream()
                .collect(Collectors.toMap(GuaranteeDto::code, GuaranteeDto::description));

        // Validate all codes and enrich descriptions from catalog
        List<CoverageOption> enriched = command.coverageOptions().stream()
                .map(option -> {
                    if (!catalogByCode.containsKey(option.code())) {
                        throw new InvalidCoverageCodeException(option.code());
                    }
                    return new CoverageOption(
                            option.code(),
                            catalogByCode.get(option.code()),
                            option.selected(),
                            option.deductiblePercentage(),
                            option.coinsurancePercentage()
                    );
                })
                .toList();

        List<CoverageOption> saved = coverageOptionRepository.replaceAll(command.folioNumber(), enriched);
        long newVersion = quoteLookupPort.getCurrentVersion(command.folioNumber());
        java.time.Instant updatedAt = quoteLookupPort.getUpdatedAt(command.folioNumber());

        return new CoverageOptionsResponse(command.folioNumber(), saved, updatedAt, newVersion);
    }
}
