package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

// Tests for SaveCoverageOptionsUseCaseImpl — TDD RED phase
@ExtendWith(MockitoExtension.class)
class SaveCoverageOptionsUseCaseImplTest {

    @Mock
    private QuoteLookupPort quoteLookupPort;

    @Mock
    private CoverageOptionRepository coverageOptionRepository;

    @Mock
    private GuaranteeCatalogClient guaranteeCatalogClient;

    @InjectMocks
    private SaveCoverageOptionsUseCaseImpl useCase;

    private static final String FOLIO = "FOL-2026-00042";

    // --- CRITERIO-2.3: version conflict → throws VersionConflictException without persisting ---

    @Test
    void shouldThrowVersionConflictException_whenVersionDoesNotMatch() {
        // GIVEN
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doThrow(new VersionConflictException(FOLIO, 6L, 7L))
                .when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(), 6L);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(VersionConflictException.class);
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    // --- CRITERIO-2.7: folio does not exist → throws FolioNotFoundException ---

    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        doThrow(new FolioNotFoundException(FOLIO)).when(quoteLookupPort).assertFolioExists(FOLIO);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(), 1L);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(FolioNotFoundException.class);
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    // --- CRITERIO-2.4: code not in catalog → throws InvalidCoverageCodeException ---

    @Test
    void shouldThrowInvalidCoverageCodeException_whenCodeNotInCatalog() {
        // GIVEN
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 5L);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(
                List.of(new GuaranteeDto("GUA-FIRE", "Incendio edificios", true))
        );
        CoverageOption invalid = new CoverageOption("COV-INVALID", null, true, BigDecimal.ZERO, BigDecimal.ZERO);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(invalid), 5L);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(InvalidCoverageCodeException.class)
                .hasMessageContaining("COV-INVALID");
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    // --- CRITERIO-2.1: successful save → returns enriched options with version+1 ---

    @Test
    void shouldReturnEnrichedOptions_whenSaveIsSuccessful() {
        // GIVEN
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(List.of(
                new GuaranteeDto("GUA-FIRE", "Incendio edificios", true),
                new GuaranteeDto("GUA-THEFT", "Robo", true)
        ));
        List<CoverageOption> input = List.of(
                new CoverageOption("GUA-FIRE", null, true, new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("GUA-THEFT", null, true, new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        List<CoverageOption> persisted = List.of(
                new CoverageOption("GUA-FIRE", "Incendio edificios", true, new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("GUA-THEFT", "Robo", true, new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        when(coverageOptionRepository.replaceAll(eq(FOLIO), any())).thenReturn(persisted);
        when(quoteLookupPort.getCurrentVersion(FOLIO)).thenReturn(7L);

        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        // WHEN
        CoverageOptionsResponse response = useCase.saveCoverageOptions(command);

        // THEN
        assertThat(response.folioNumber()).isEqualTo(FOLIO);
        assertThat(response.coverageOptions()).hasSize(2);
        assertThat(response.coverageOptions().get(0).description()).isEqualTo("Incendio edificios");
        assertThat(response.coverageOptions().get(1).description()).isEqualTo("Robo");
        assertThat(response.version()).isEqualTo(7L);

        // verify the enriched list was passed to replaceAll
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CoverageOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(coverageOptionRepository).replaceAll(eq(FOLIO), captor.capture());
        assertThat(captor.getValue().get(0).description()).isEqualTo("Incendio edificios");
        assertThat(captor.getValue().get(1).description()).isEqualTo("Robo");
    }

    // --- R-004: duplicate codes in request → throws InvalidCoverageCodeException without persisting ---

    @Test
    void shouldThrowInvalidCoverageCodeException_whenRequestContainsDuplicateCodes() {
        // GIVEN — duplicate check runs before catalog fetch, so no stub needed for fetchGuarantees
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        List<CoverageOption> input = List.of(
                new CoverageOption("GUA-FIRE", null, true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("GUA-FIRE", null, false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(InvalidCoverageCodeException.class)
                .hasMessageContaining("GUA-FIRE");
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
        verify(guaranteeCatalogClient, never()).fetchGuarantees();
    }

    // --- CRITERIO-2.2: mix of selected true/false → both states persisted correctly ---

    @Test
    void shouldPersistBothSelectedStates_whenMixOfTrueAndFalse() {
        // GIVEN
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        when(guaranteeCatalogClient.fetchGuarantees()).thenReturn(List.of(
                new GuaranteeDto("GUA-FIRE", "Incendio edificios", true),
                new GuaranteeDto("GUA-THEFT", "Robo", true)
        ));
        List<CoverageOption> input = List.of(
                new CoverageOption("GUA-FIRE", null, true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("GUA-THEFT", null, false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        List<CoverageOption> persisted = List.of(
                new CoverageOption("GUA-FIRE", "Incendio edificios", true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("GUA-THEFT", "Robo", false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(coverageOptionRepository.replaceAll(eq(FOLIO), any())).thenReturn(persisted);
        when(quoteLookupPort.getCurrentVersion(FOLIO)).thenReturn(7L);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        // WHEN
        CoverageOptionsResponse response = useCase.saveCoverageOptions(command);

        // THEN
        assertThat(response.coverageOptions().get(0).selected()).isTrue();
        assertThat(response.coverageOptions().get(1).selected()).isFalse();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CoverageOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(coverageOptionRepository).replaceAll(eq(FOLIO), captor.capture());
        assertThat(captor.getValue().get(0).selected()).isTrue();
        assertThat(captor.getValue().get(1).selected()).isFalse();
    }
}
