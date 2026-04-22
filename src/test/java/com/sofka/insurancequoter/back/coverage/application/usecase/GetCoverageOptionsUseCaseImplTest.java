package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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

    @InjectMocks
    private GetCoverageOptionsUseCaseImpl useCase;

    // --- CRITERIO-1.1: folio exists with options → returns populated list ---

    @Test
    void shouldReturnOptions_whenFolioExistsWithCoverageOptions() {
        // GIVEN
        String folio = "FOL-2026-00042";
        List<CoverageOption> options = List.of(
                new CoverageOption("GUA-FIRE", "Incendio edificios", true,
                        new BigDecimal("2.0"), new BigDecimal("80.0")),
                new CoverageOption("GUA-THEFT", "Robo", false,
                        new BigDecimal("5.0"), new BigDecimal("100.0"))
        );
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(options);
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(6L);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.folioNumber()).isEqualTo(folio);
        assertThat(response.coverageOptions()).hasSize(2);
        assertThat(response.coverageOptions().get(0).code()).isEqualTo("GUA-FIRE");
        assertThat(response.coverageOptions().get(0).selected()).isTrue();
        assertThat(response.coverageOptions().get(1).code()).isEqualTo("GUA-THEFT");
        assertThat(response.coverageOptions().get(1).selected()).isFalse();
        assertThat(response.version()).isEqualTo(6L);
        verify(quoteLookupPort).assertFolioExists(folio);
        verify(coverageOptionRepository).findByFolioNumber(folio);
        verify(quoteLookupPort).getCurrentVersion(folio);
    }

    // --- CRITERIO-1.2: folio exists without options → returns empty list ---

    @Test
    void shouldReturnEmptyList_whenFolioExistsWithNoCoverageOptions() {
        // GIVEN
        String folio = "FOL-2026-00042";
        doNothing().when(quoteLookupPort).assertFolioExists(folio);
        when(coverageOptionRepository.findByFolioNumber(folio)).thenReturn(List.of());
        when(quoteLookupPort.getCurrentVersion(folio)).thenReturn(3L);

        // WHEN
        CoverageOptionsResponse response = useCase.getCoverageOptions(folio);

        // THEN
        assertThat(response.folioNumber()).isEqualTo(folio);
        assertThat(response.coverageOptions()).isEmpty();
        assertThat(response.version()).isEqualTo(3L);
    }

    // --- CRITERIO-1.3: folio does not exist → throws FolioNotFoundException ---

    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        String folio = "FOL-9999-00001";
        doThrow(new FolioNotFoundException(folio)).when(quoteLookupPort).assertFolioExists(folio);

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getCoverageOptions(folio))
                .isInstanceOf(FolioNotFoundException.class);
        verify(coverageOptionRepository, never()).findByFolioNumber(any());
    }
}
