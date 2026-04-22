package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.NoCalculableLocationsException;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.calculation.domain.port.in.CalculatePremiumUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.out.CalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.QuoteCalculationReader;
import com.sofka.insurancequoter.back.calculation.domain.port.out.TariffClient;
import com.sofka.insurancequoter.back.calculation.domain.service.CalculationService;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

// Orchestrates premium calculation: snapshot → version check → tariffs → calculate → persist
public class CalculatePremiumUseCaseImpl implements CalculatePremiumUseCase {

    private final QuoteCalculationReader quoteCalculationReader;
    private final CalculationResultRepository calculationResultRepository;
    private final TariffClient tariffClient;
    private final GuaranteeCatalogClient guaranteeCatalogClient;
    private final CalculationService calculationService;

    public CalculatePremiumUseCaseImpl(QuoteCalculationReader quoteCalculationReader,
                                       CalculationResultRepository calculationResultRepository,
                                       TariffClient tariffClient,
                                       GuaranteeCatalogClient guaranteeCatalogClient,
                                       CalculationService calculationService) {
        this.quoteCalculationReader = quoteCalculationReader;
        this.calculationResultRepository = calculationResultRepository;
        this.tariffClient = tariffClient;
        this.guaranteeCatalogClient = guaranteeCatalogClient;
        this.calculationService = calculationService;
    }

    @Override
    public CalculationResult calculate(CalculatePremiumCommand command) {
        // 1. Load snapshot — throws FolioNotFoundException if not found
        QuoteCalculationSnapshot snapshot = quoteCalculationReader.getSnapshot(command.folioNumber());

        // 2. Validate optimistic lock version
        if (snapshot.version() != command.version()) {
            throw new VersionConflictException(command.folioNumber(), command.version(), snapshot.version());
        }

        // 3. Fetch tariffs from core service
        Tariff tariff = tariffClient.fetchTariffs();

        // 4. Build set of tarifable guarantee codes from catalog
        Set<String> tarifableCodes = guaranteeCatalogClient.fetchGuarantees().stream()
                .filter(g -> g.tarifable())
                .map(g -> g.code())
                .collect(Collectors.toSet());

        // 5. Calculate premium for each location
        List<PremiumByLocation> premiumsByLocation = snapshot.locations().stream()
                .map(loc -> calculationService.calculateLocation(loc, tariff, tarifableCodes))
                .toList();

        // 6. Validate at least one calculable location
        boolean hasCalculable = premiumsByLocation.stream().anyMatch(PremiumByLocation::calculable);
        if (!hasCalculable) {
            throw new NoCalculableLocationsException();
        }

        // 7. Consolidate total net premium from calculable locations only
        BigDecimal netPremium = premiumsByLocation.stream()
                .filter(PremiumByLocation::calculable)
                .map(PremiumByLocation::netPremium)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 8. Calculate commercial premium
        BigDecimal commercialPremium = netPremium
                .multiply(tariff.commercialFactor())
                .setScale(2, RoundingMode.HALF_UP);

        // 9. Build result (version=0 placeholder before persist)
        CalculationResult result = new CalculationResult(
                command.folioNumber(),
                netPremium,
                commercialPremium,
                premiumsByLocation,
                Instant.now(),
                0L
        );

        // 10. Persist atomically; returns the new quote version after @Version increment
        long newVersion = calculationResultRepository.persist(command.folioNumber(), result);

        // 11. Return result with the real version
        return new CalculationResult(
                result.folioNumber(),
                result.netPremium(),
                result.commercialPremium(),
                result.premiumsByLocation(),
                result.calculatedAt(),
                newVersion
        );
    }
}
