package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.GetCalculationResultUseCase;
import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;
import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteState;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteSections;
import com.sofka.insurancequoter.back.folio.domain.model.SectionStatus;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SuppressWarnings("java:S100")
@ExtendWith(MockitoExtension.class)
class ListFoliosUseCaseImplTest {

    @Mock
    private FolioListQuery folioListQuery;

    @Mock
    private GetQuoteStateUseCase getQuoteStateUseCase;

    @Mock
    private CoreServiceClient coreServiceClient;

    @Mock
    private GetCalculationResultUseCase getCalculationResultUseCase;

    private ListFoliosUseCase useCase;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new ListFoliosUseCaseImpl(folioListQuery, getQuoteStateUseCase, coreServiceClient,
                getCalculationResultUseCase);
    }

    // --- Happy path: lista con un folio, con prima comercial calculada ---

    @Test
    void shouldReturnFolioSummaries_whenFoliosExist() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00001", "Empresa Alfa SA de CV",
                "AGT-001", "CREATED", 1, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-001")).thenReturn("Carlos López");
        when(getQuoteStateUseCase.getState("FOL-2026-00001")).thenReturn(buildState("FOL-2026-00001", 10));
        when(getCalculationResultUseCase.get("FOL-2026-00001"))
                .thenReturn(buildCalculationResult("FOL-2026-00001", new BigDecimal("12500.00")));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result).hasSize(1);
        FolioSummary summary = result.get(0);
        assertThat(summary.folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(summary.client()).isEqualTo("Empresa Alfa SA de CV");
        assertThat(summary.agentCode()).isEqualTo("AGT-001");
        assertThat(summary.agentName()).isEqualTo("Carlos López");
        assertThat(summary.status()).isEqualTo("CREATED");
        assertThat(summary.locationCount()).isEqualTo(1);
        assertThat(summary.completionPct()).isEqualTo(10);
        assertThat(summary.commercialPremium()).isEqualByComparingTo(new BigDecimal("12500.00"));
        assertThat(summary.updatedAt()).isEqualTo(NOW);
    }

    // --- Empty list ---

    @Test
    void shouldReturnEmptyList_whenNoFoliosExist() {
        // GIVEN
        when(folioListQuery.findAll()).thenReturn(List.of());

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result).isEmpty();
    }

    // --- Null insuredName maps to null client ---

    @Test
    void shouldReturnNullClient_whenInsuredNameIsNull() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00002", null, "AGT-002", "CREATED", 0, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-002")).thenReturn("Ana Gómez");
        when(getQuoteStateUseCase.getState("FOL-2026-00002")).thenReturn(buildState("FOL-2026-00002", 0));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).client()).isNull();
    }

    // --- Null numberOfLocations falls back to 0 ---

    @Test
    void shouldReturnZeroLocationCount_whenNumberOfLocationsIsNull() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00003", "Corp X", "AGT-003", "CREATED", 0, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-003")).thenReturn("Pedro Ruiz");
        when(getQuoteStateUseCase.getState("FOL-2026-00003")).thenReturn(buildState("FOL-2026-00003", 0));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result.get(0).locationCount()).isZero();
    }

    // --- Core service unavailable: agentName falls back to null, does not crash ---

    @Test
    void shouldReturnNullAgentName_whenCoreServiceThrows() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00004", "Corp Y", "AGT-004", "CREATED", 1, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-004"))
                .thenThrow(new CoreServiceException("Core unavailable"));
        when(getQuoteStateUseCase.getState("FOL-2026-00004")).thenReturn(buildState("FOL-2026-00004", 10));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).agentName()).isNull();
    }

    // --- completionPct comes from GetQuoteStateUseCase ---

    @Test
    void shouldUseCompletionPctFromGetQuoteStateUseCase() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00005", "Corp Z", "AGT-005", "IN_PROGRESS", 2, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-005")).thenReturn("Laura Vega");
        when(getQuoteStateUseCase.getState("FOL-2026-00005")).thenReturn(buildState("FOL-2026-00005", 60));
        when(getCalculationResultUseCase.get("FOL-2026-00005"))
                .thenReturn(buildCalculationResult("FOL-2026-00005", new BigDecimal("5000.00")));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result.get(0).completionPct()).isEqualTo(60);
    }

    // --- commercialPremium populated from GetCalculationResultUseCase ---

    @Test
    void shouldPopulateCommercialPremium_whenCalculationResultExists() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00006", "Corp W", "AGT-006", "IN_PROGRESS", 3, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-006")).thenReturn("Mario Flores");
        when(getQuoteStateUseCase.getState("FOL-2026-00006")).thenReturn(buildState("FOL-2026-00006", 80));
        when(getCalculationResultUseCase.get("FOL-2026-00006"))
                .thenReturn(buildCalculationResult("FOL-2026-00006", new BigDecimal("98765.43")));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result.get(0).commercialPremium()).isEqualByComparingTo(new BigDecimal("98765.43"));
    }

    // --- commercialPremium is null when calculation result is not available ---

    @Test
    void shouldReturnNullCommercialPremium_whenGetCalculationResultUseCaseThrows() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00007", "Corp V", "AGT-007", "CREATED", 0, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-007")).thenReturn("Sofia Ramos");
        when(getQuoteStateUseCase.getState("FOL-2026-00007")).thenReturn(buildState("FOL-2026-00007", 0));
        when(getCalculationResultUseCase.get("FOL-2026-00007"))
                .thenThrow(new RuntimeException("Calculation result not found"));

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).commercialPremium()).isNull();
    }

    // --- Helpers ---

    private QuoteState buildState(String folioNumber, int completionPct) {
        QuoteSections sections = new QuoteSections(
                SectionStatus.PENDING, SectionStatus.PENDING,
                SectionStatus.PENDING, SectionStatus.PENDING, SectionStatus.PENDING);
        return new QuoteState(folioNumber, "CREATED", completionPct, sections, 1L, NOW);
    }

    private CalculationResult buildCalculationResult(String folioNumber, BigDecimal commercialPremium) {
        return new CalculationResult(folioNumber, commercialPremium.multiply(new BigDecimal("0.9")),
                commercialPremium, List.of(), NOW, 1L);
    }
}
