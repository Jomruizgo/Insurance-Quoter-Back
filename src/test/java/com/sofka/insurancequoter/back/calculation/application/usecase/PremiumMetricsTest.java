package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.NoCalculableLocationsException;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.calculation.domain.port.out.CalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.QuoteCalculationReader;
import com.sofka.insurancequoter.back.calculation.domain.port.out.TariffClient;
import com.sofka.insurancequoter.back.calculation.domain.service.CalculationService;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PremiumMetricsTest {

    @Mock private QuoteCalculationReader quoteCalculationReader;
    @Mock private CalculationResultRepository calculationResultRepository;
    @Mock private TariffClient tariffClient;
    @Mock private GuaranteeCatalogClient guaranteeCatalogClient;
    @Mock private CalculationService calculationService;

    private SimpleMeterRegistry meterRegistry;
    private CalculatePremiumUseCaseImpl useCase;

    private static final Tariff TARIFF = new Tariff(
            BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001), BigDecimal.ONE,
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
            BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001),
            BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001), BigDecimal.valueOf(0.001),
            BigDecimal.valueOf(0.001), BigDecimal.valueOf(1.1)
    );

    private static final Location DUMMY_LOCATION = new Location(
            0, true, "loc1", "addr", "12345", "ST", "MUN",
            "COL", "CITY", "C1", 1, 2000, null, List.of(),
            null, ValidationStatus.COMPLETE, List.of()
    );

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new CalculatePremiumUseCaseImpl(
                quoteCalculationReader, calculationResultRepository,
                tariffClient, guaranteeCatalogClient, calculationService, meterRegistry);
    }

    @Test
    void premiumCalculatedCounter_increments_whenCalculationSucceeds() {
        // GIVEN
        QuoteCalculationSnapshot snapshot = new QuoteCalculationSnapshot("FOL-001", 1L,
                List.of(DUMMY_LOCATION), List.of());
        PremiumByLocation calculable = new PremiumByLocation(0, "loc1",
                BigDecimal.valueOf(100), BigDecimal.valueOf(110), true, null, List.of());

        when(quoteCalculationReader.getSnapshot("FOL-001")).thenReturn(snapshot);
        when(tariffClient.fetchTariffs()).thenReturn(TARIFF);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(List.of());
        when(calculationService.calculateLocation(any(), any(), anySet())).thenReturn(calculable);
        when(calculationResultRepository.persist(any(), any())).thenReturn(2L);

        // WHEN
        useCase.calculate(new CalculatePremiumCommand("FOL-001", 1L));

        // THEN
        Counter counter = meterRegistry.find("premium.calculated").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void calculationErrorCounter_increments_whenNoCalculableLocations() {
        // GIVEN
        QuoteCalculationSnapshot snapshot = new QuoteCalculationSnapshot("FOL-001", 1L,
                List.of(DUMMY_LOCATION), List.of());
        PremiumByLocation nonCalculable = new PremiumByLocation(0, "loc1",
                null, null, false, null, List.of());

        when(quoteCalculationReader.getSnapshot("FOL-001")).thenReturn(snapshot);
        when(tariffClient.fetchTariffs()).thenReturn(TARIFF);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(List.of());
        when(calculationService.calculateLocation(any(), any(), anySet())).thenReturn(nonCalculable);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.calculate(new CalculatePremiumCommand("FOL-001", 1L)))
                .isInstanceOf(NoCalculableLocationsException.class);

        Counter errorCounter = meterRegistry.find("calculation.errors")
                .tag("errorType", "NoCalculableLocations").counter();
        assertThat(errorCounter).isNotNull();
        assertThat(errorCounter.count()).isEqualTo(1.0);
    }
}
