package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveCoverageOptionsUseCaseImplTest {

    @Mock
    private QuoteLookupPort quoteLookupPort;

    @Mock
    private CoverageOptionRepository coverageOptionRepository;

    @Mock
    private CoverageDerivationService coverageDerivationService;

    @InjectMocks
    private SaveCoverageOptionsUseCaseImpl useCase;

    private static final String FOLIO = "FOL-2026-00042";

    private static final Map<String, String> KNOWN = Map.of(
            "COV-FIRE", "Incendio y riesgos adicionales",
            "COV-THEFT", "Robo con violencia",
            "COV-GLASS", "Vidrios",
            "COV-ELEC", "Equipo electronico",
            "COV-CAT", "Zona catastrofica",
            "COV-BI", "Interrupcion de negocio"
    );

    @Test
    void shouldThrowVersionConflictException_whenVersionDoesNotMatch() {
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doThrow(new VersionConflictException(FOLIO, 6L, 7L))
                .when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(), 6L);

        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(VersionConflictException.class);
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        doThrow(new FolioNotFoundException(FOLIO)).when(quoteLookupPort).assertFolioExists(FOLIO);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(), 1L);

        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(FolioNotFoundException.class);
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    @Test
    void shouldThrowInvalidCoverageCodeException_whenCodeNotInCatalog() {
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 5L);
        when(coverageDerivationService.knownDescriptions()).thenReturn(KNOWN);
        CoverageOption invalid = new CoverageOption("COV-INVALID", null, true, BigDecimal.ZERO, BigDecimal.ZERO);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, List.of(invalid), 5L);

        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(InvalidCoverageCodeException.class)
                .hasMessageContaining("COV-INVALID");
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
    }

    @Test
    void shouldReturnEnrichedOptions_whenSaveIsSuccessful() {
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        when(coverageDerivationService.knownDescriptions()).thenReturn(KNOWN);
        List<CoverageOption> input = List.of(
                new CoverageOption("COV-FIRE", null, true, new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("COV-THEFT", null, true, new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        List<CoverageOption> persisted = List.of(
                new CoverageOption("COV-FIRE", "Incendio y riesgos adicionales", true, new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("COV-THEFT", "Robo con violencia", true, new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        when(coverageOptionRepository.replaceAll(eq(FOLIO), any())).thenReturn(persisted);
        when(quoteLookupPort.getCurrentVersion(FOLIO)).thenReturn(7L);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        CoverageOptionsResponse response = useCase.saveCoverageOptions(command);

        assertThat(response.folioNumber()).isEqualTo(FOLIO);
        assertThat(response.coverageOptions()).hasSize(2);
        assertThat(response.coverageOptions().get(0).description()).isEqualTo("Incendio y riesgos adicionales");
        assertThat(response.coverageOptions().get(1).description()).isEqualTo("Robo con violencia");
        assertThat(response.version()).isEqualTo(7L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CoverageOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(coverageOptionRepository).replaceAll(eq(FOLIO), captor.capture());
        assertThat(captor.getValue().get(0).description()).isEqualTo("Incendio y riesgos adicionales");
        assertThat(captor.getValue().get(1).description()).isEqualTo("Robo con violencia");
    }

    @Test
    void shouldThrowInvalidCoverageCodeException_whenRequestContainsDuplicateCodes() {
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        List<CoverageOption> input = List.of(
                new CoverageOption("COV-FIRE", null, true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("COV-FIRE", null, false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        assertThatThrownBy(() -> useCase.saveCoverageOptions(command))
                .isInstanceOf(InvalidCoverageCodeException.class)
                .hasMessageContaining("COV-FIRE");
        verify(coverageOptionRepository, never()).replaceAll(any(), any());
        verify(coverageDerivationService, never()).knownDescriptions();
    }

    @Test
    void shouldPersistBothSelectedStates_whenMixOfTrueAndFalse() {
        doNothing().when(quoteLookupPort).assertFolioExists(FOLIO);
        doNothing().when(quoteLookupPort).assertVersionMatches(FOLIO, 6L);
        when(coverageDerivationService.knownDescriptions()).thenReturn(KNOWN);
        List<CoverageOption> input = List.of(
                new CoverageOption("COV-FIRE", null, true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("COV-THEFT", null, false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        List<CoverageOption> persisted = List.of(
                new CoverageOption("COV-FIRE", "Incendio y riesgos adicionales", true, BigDecimal.ZERO, BigDecimal.ZERO),
                new CoverageOption("COV-THEFT", "Robo con violencia", false, BigDecimal.ZERO, BigDecimal.ZERO)
        );
        when(coverageOptionRepository.replaceAll(eq(FOLIO), any())).thenReturn(persisted);
        when(quoteLookupPort.getCurrentVersion(FOLIO)).thenReturn(7L);
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(FOLIO, input, 6L);

        CoverageOptionsResponse response = useCase.saveCoverageOptions(command);

        assertThat(response.coverageOptions().get(0).selected()).isTrue();
        assertThat(response.coverageOptions().get(1).selected()).isFalse();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CoverageOption>> captor = ArgumentCaptor.forClass(List.class);
        verify(coverageOptionRepository).replaceAll(eq(FOLIO), captor.capture());
        assertThat(captor.getValue().get(0).selected()).isTrue();
        assertThat(captor.getValue().get(1).selected()).isFalse();
    }
}
