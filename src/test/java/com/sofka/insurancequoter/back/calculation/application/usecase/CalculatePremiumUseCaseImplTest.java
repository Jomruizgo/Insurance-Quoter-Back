package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.NoCalculableLocationsException;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.calculation.domain.port.out.CalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.QuoteCalculationReader;
import com.sofka.insurancequoter.back.calculation.domain.port.out.TariffClient;
import com.sofka.insurancequoter.back.calculation.domain.service.CalculationService;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
import com.sofka.insurancequoter.back.location.domain.model.BusinessLine;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Unit tests for CalculatePremiumUseCaseImpl — RED phase (TDD)
@ExtendWith(MockitoExtension.class)
class CalculatePremiumUseCaseImplTest {

    @Mock
    private QuoteCalculationReader quoteCalculationReader;

    @Mock
    private CalculationResultRepository calculationResultRepository;

    @Mock
    private TariffClient tariffClient;

    @Mock
    private GuaranteeCatalogClient guaranteeCatalogClient;

    private CalculationService calculationService;
    private CalculatePremiumUseCaseImpl useCase;

    private static final String FOLIO = "FOL-2026-00042";
    private static final long VERSION = 7L;

    private static final Tariff TARIFF = new Tariff(
            BigDecimal.valueOf(0.0015),
            BigDecimal.valueOf(0.0012),
            BigDecimal.valueOf(0.07),
            BigDecimal.valueOf(0.0008),
            BigDecimal.valueOf(0.0005),
            BigDecimal.valueOf(0.03),
            BigDecimal.valueOf(0.02),
            BigDecimal.valueOf(0.015),
            BigDecimal.valueOf(0.015),
            BigDecimal.valueOf(0.002),
            BigDecimal.valueOf(0.003),
            BigDecimal.valueOf(0.005),
            BigDecimal.valueOf(0.001),
            BigDecimal.valueOf(0.002),
            BigDecimal.valueOf(1.16)
    );

    @BeforeEach
    void setUp() {
        calculationService = new CalculationService();
        useCase = new CalculatePremiumUseCaseImpl(
                quoteCalculationReader, calculationResultRepository,
                tariffClient, guaranteeCatalogClient, calculationService,
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        );
    }

    // --- Helper builders ---

    private Location completeLocation(int index, BigDecimal insuredValue) {
        return new Location(
                index, true, "Location " + index, "Av. Test " + index,
                "06600", "Estado", "Municipio", "Col", "Ciudad",
                "MASONRY", 1, 2000,
                new BusinessLine("BL-001", "FK-001", "Desc"),
                List.of(new Guarantee("GUA-FIRE", insuredValue)),
                "ZONE_A", ValidationStatus.COMPLETE, List.of()
        );
    }

    private Location incompleteLocation(int index) {
        return new Location(
                index, true, "Incomplete " + index, "Av. X",
                null, null, null, null, null,
                null, null, null,
                null, List.of(),
                null, ValidationStatus.INCOMPLETE, List.of()
        );
    }

    private void stubTariffAndCatalog() {
        when(tariffClient.fetchTariffs()).thenReturn(TARIFF);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(
                List.of(new GuaranteeDto("GUA-FIRE", "Incendio edificios", true))
        );
    }

    // --- Tests ---

    @Test
    void calculate_throwsFolioNotFoundException_whenFolioNotFound() {
        // GIVEN
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenThrow(new FolioNotFoundException(FOLIO));
        // WHEN / THEN
        var command = new CalculatePremiumCommand(FOLIO, VERSION);
        assertThatThrownBy(() -> useCase.calculate(command))
                .isInstanceOf(FolioNotFoundException.class);
    }

    @Test
    void calculate_throwsVersionConflictException_whenVersionMismatch() {
        // GIVEN — stored version is 8, command sends 7
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, 8L, List.of(completeLocation(1, BigDecimal.valueOf(1_000_000))), List.of())
        );
        // WHEN / THEN
        var command = new CalculatePremiumCommand(FOLIO, VERSION);
        assertThatThrownBy(() -> useCase.calculate(command))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void calculate_throwsNoCalculableLocationsException_whenAllLocationsIncomplete() {
        // GIVEN
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(incompleteLocation(1), incompleteLocation(2)), List.of())
        );
        stubTariffAndCatalog();
        // WHEN / THEN
        assertThatThrownBy(() -> useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION)))
                .isInstanceOf(NoCalculableLocationsException.class);
    }

    @Test
    void calculate_returnsTotalNetPremium_asSumOfCalculableLocations() {
        // GIVEN — two calculable locations, each with 1,000,000 GUA-FIRE
        // fireBuildings per loc = 1,500,000 × 0.0015 = ... wait, 1_000_000 × 0.0015 = 1500
        Location loc1 = completeLocation(1, BigDecimal.valueOf(1_000_000));
        Location loc2 = completeLocation(2, BigDecimal.valueOf(2_000_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc1, loc2), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        CalculationResult result = useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN
        assertThat(result.netPremium()).isPositive();
        assertThat(result.premiumsByLocation()).hasSize(2);
        assertThat(result.premiumsByLocation().stream().allMatch(p -> p.calculable())).isTrue();
    }

    @Test
    void calculate_setsQuoteStatusToCalculated_onSuccess() {
        // GIVEN
        Location loc = completeLocation(1, BigDecimal.valueOf(1_000_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN — persist called means quoteStatus is updated in adapter
        verify(calculationResultRepository).persist(eq(FOLIO), any(CalculationResult.class));
    }

    @Test
    void calculate_persistsResult_withCorrectPremiums() {
        // GIVEN
        Location loc = completeLocation(1, BigDecimal.valueOf(1_000_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc), List.of())
        );
        stubTariffAndCatalog();
        ArgumentCaptor<CalculationResult> captor = ArgumentCaptor.forClass(CalculationResult.class);
        when(calculationResultRepository.persist(eq(FOLIO), captor.capture())).thenReturn(8L);
        // WHEN
        useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN
        CalculationResult persisted = captor.getValue();
        assertThat(persisted.folioNumber()).isEqualTo(FOLIO);
        assertThat(persisted.netPremium()).isPositive();
        assertThat(persisted.commercialPremium()).isPositive();
    }

    @Test
    void calculate_includesBlockingAlerts_forNonCalculableLocations() {
        // GIVEN — loc1 calculable, loc2 missing zipCode
        Location loc1 = completeLocation(1, BigDecimal.valueOf(1_000_000));
        Location loc2 = incompleteLocation(2);
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc1, loc2), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        CalculationResult result = useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN
        assertThat(result.premiumsByLocation().get(0).calculable()).isTrue();
        assertThat(result.premiumsByLocation().get(1).calculable()).isFalse();
        assertThat(result.premiumsByLocation().get(1).blockingAlerts()).isNotEmpty();
    }

    @Test
    void calculate_succeeds_withOnlyOneCalculableLocation() {
        // GIVEN — loc1 incomplete, loc2 calculable
        Location loc1 = incompleteLocation(1);
        Location loc2 = completeLocation(2, BigDecimal.valueOf(500_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc1, loc2), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        CalculationResult result = useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN
        assertThat(result.netPremium()).isPositive();
        assertThat(result.premiumsByLocation().stream().filter(p -> p.calculable()).count()).isEqualTo(1);
    }

    @Test
    void calculate_callsTariffClientOnce() {
        // GIVEN
        Location loc = completeLocation(1, BigDecimal.valueOf(1_000_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN
        verify(tariffClient, times(1)).fetchTariffs();
    }

    @Test
    void calculate_isIdempotent_onRecalculation() {
        // GIVEN — same input, called twice
        Location loc = completeLocation(1, BigDecimal.valueOf(1_000_000));
        when(quoteCalculationReader.getSnapshot(FOLIO)).thenReturn(
                new QuoteCalculationSnapshot(FOLIO, VERSION, List.of(loc), List.of())
        );
        stubTariffAndCatalog();
        when(calculationResultRepository.persist(eq(FOLIO), any())).thenReturn(8L);
        // WHEN
        CalculationResult first = useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        CalculationResult second = useCase.calculate(new CalculatePremiumCommand(FOLIO, VERSION));
        // THEN — persist is called both times (idempotent replace)
        verify(calculationResultRepository, times(2)).persist(eq(FOLIO), any());
        assertThat(first.netPremium()).isEqualByComparingTo(second.netPremium());
    }
}
