package com.sofka.insurancequoter.back.folio.application.usecase;

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

    private ListFoliosUseCase useCase;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    void setUp() {
        useCase = new ListFoliosUseCaseImpl(folioListQuery, getQuoteStateUseCase, coreServiceClient);
    }

    // --- Happy path: lista con un folio ---

    @Test
    void shouldReturnFolioSummaries_whenFoliosExist() {
        // GIVEN
        FolioRaw raw = new FolioRaw("FOL-2026-00001", "Empresa Alfa SA de CV",
                "AGT-001", "CREATED", 1, NOW);
        when(folioListQuery.findAll()).thenReturn(List.of(raw));
        when(coreServiceClient.getAgentName("AGT-001")).thenReturn("Carlos López");

        QuoteState state = buildState("FOL-2026-00001", 10);
        when(getQuoteStateUseCase.getState("FOL-2026-00001")).thenReturn(state);

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
        assertThat(summary.commercialPremium()).isNull();
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

        // WHEN
        List<FolioSummary> result = useCase.listFolios();

        // THEN
        assertThat(result.get(0).completionPct()).isEqualTo(60);
    }

    // --- Helper ---

    private QuoteState buildState(String folioNumber, int completionPct) {
        QuoteSections sections = new QuoteSections(
                SectionStatus.PENDING, SectionStatus.PENDING,
                SectionStatus.PENDING, SectionStatus.PENDING, SectionStatus.PENDING);
        return new QuoteState(folioNumber, "CREATED", completionPct, sections, 1L, NOW);
    }
}
