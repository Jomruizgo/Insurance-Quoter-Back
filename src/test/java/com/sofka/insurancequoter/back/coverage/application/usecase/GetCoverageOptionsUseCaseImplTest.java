package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.ActiveGuaranteeReader;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

// Tests for GetCoverageOptionsUseCaseImpl — TDD RED phase
@ExtendWith(MockitoExtension.class)
class GetCoverageOptionsUseCaseImplTest {

    @Mock
    private CoverageOptionRepository coverageOptionRepository;

    @Mock
    private QuoteLookupPort quoteLookupPort;

    @Mock
    private ActiveGuaranteeReader activeGuaranteeReader;

    // CoverageDerivationService is a pure domain class — use real instance for unit accuracy
    private CoverageDerivationService coverageDerivationService;

    private GetCoverageOptionsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        coverageDerivationService = new CoverageDerivationService();
        useCase = new GetCoverageOptionsUseCaseImpl(
                quoteLookupPort,
                coverageOptionRepository,
                activeGuaranteeReader,
                coverageDerivationService
        );
    }

    // --- CRITERIO-1.1: folio exists with saved options → returns persisted list ---

    @Test
    void shouldReturnPersistedOptions_whenFolioExistsWithSavedCoverageOptions() {
        // GIVEN
        String folio = "FOL-2026-00042";
        Instant updatedAt = Instant.parse("2026-04-20T15:00:00Z");
        List<CoverageOption> options = List.of(
                new CoverageOption("COV-FIRE", "Incendio y riesgos adicionales", true,
                        new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("COV-THEFT", "Robo con violencia", false,
                        new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(options);
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(6L);
        when(quoteLookupPort.getUpdatedAt(folio)).thenReturn(updatedAt);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.folioNumber()).isEqualTo(folio);
        assertThat(response.coverageOptions()).hasSize(2);
        assertThat(response.coverageOptions().get(0).code()).isEqualTo("COV-FIRE");
        assertThat(response.coverageOptions().get(0).selected()).isTrue();
        assertThat(response.coverageOptions().get(1).code()).isEqualTo("COV-THEFT");
        assertThat(response.version()).isEqualTo(6L);
        // activeGuaranteeReader must NOT be called when options exist in DB
        verify(activeGuaranteeReader, never()).readActiveGuaranteeCodes(any());
        verify(activeGuaranteeReader, never()).hasCatastrophicZone(any());
    }

    // --- CRITERIO-1.2: folio exists with empty options → derive from guarantees ---

    @Test
    void shouldDeriveOptions_whenFolioExistsWithNoCoverageOptionsInDb() {
        // GIVEN
        String folio = "FOL-2026-00042";
        Instant updatedAt = Instant.parse("2026-04-20T15:00:00Z");
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(List.of());
        when(activeGuaranteeReader.readActiveGuaranteeCodes(folio))
                .thenReturn(List.of("GUA-FIRE", "GUA-THEFT"));
        when(activeGuaranteeReader.hasCatastrophicZone(folio)).thenReturn(false);
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(3L);
        when(quoteLookupPort.getUpdatedAt(folio)).thenReturn(updatedAt);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.folioNumber()).isEqualTo(folio);
        assertThat(response.coverageOptions()).isNotEmpty();
        // COV-FIRE derived from GUA-FIRE
        assertThat(response.coverageOptions().stream().anyMatch(o -> "COV-FIRE".equals(o.code()))).isTrue();
        // COV-THEFT derived from GUA-THEFT
        assertThat(response.coverageOptions().stream().anyMatch(o -> "COV-THEFT".equals(o.code()))).isTrue();
        // COV-BI always present
        assertThat(response.coverageOptions().stream().anyMatch(o -> "COV-BI".equals(o.code()))).isTrue();
        assertThat(response.version()).isEqualTo(3L);
        verify(activeGuaranteeReader).readActiveGuaranteeCodes(folio);
        verify(activeGuaranteeReader).hasCatastrophicZone(folio);
    }

    // --- CRITERIO-1.3: folio has no guarantees and no cat zone → only COV-BI derived ---

    @Test
    void shouldReturnOnlyCovBi_whenDbEmptyAndNoActiveGuarantees() {
        // GIVEN
        String folio = "FOL-2026-00042";
        Instant updatedAt = Instant.parse("2026-04-20T15:00:00Z");
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(List.of());
        when(activeGuaranteeReader.readActiveGuaranteeCodes(folio)).thenReturn(List.of());
        when(activeGuaranteeReader.hasCatastrophicZone(folio)).thenReturn(false);
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(2L);
        when(quoteLookupPort.getUpdatedAt(folio)).thenReturn(updatedAt);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.coverageOptions()).hasSize(1);
        assertThat(response.coverageOptions().get(0).code()).isEqualTo("COV-BI");
        assertThat(response.coverageOptions().get(0).selected()).isFalse();
    }

    // --- CRITERIO-1.4: catastrophic zone triggers COV-CAT when DB is empty ---

    @Test
    void shouldIncludeCovCat_whenDbEmptyAndHasCatastrophicZone() {
        // GIVEN
        String folio = "FOL-2026-00042";
        Instant updatedAt = Instant.parse("2026-04-20T15:00:00Z");
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(List.of());
        when(activeGuaranteeReader.readActiveGuaranteeCodes(folio)).thenReturn(List.of());
        when(activeGuaranteeReader.hasCatastrophicZone(folio)).thenReturn(true);
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(2L);
        when(quoteLookupPort.getUpdatedAt(folio)).thenReturn(updatedAt);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.coverageOptions().stream().anyMatch(o -> "COV-CAT".equals(o.code()))).isTrue();
    }

    // --- CRITERIO-1.5: folio does not exist → throws FolioNotFoundException ---

    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        String folio = "FOL-9999-00001";
        doThrow(new FolioNotFoundException(folio)).when(quoteLookupPort).assertFolioExists(folio);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getCoverageOptions(folio))
                .isInstanceOf(FolioNotFoundException.class);
        verify(coverageOptionRepository, never()).findByFolioNumber(any());
        verify(activeGuaranteeReader, never()).readActiveGuaranteeCodes(any());
    }
}
