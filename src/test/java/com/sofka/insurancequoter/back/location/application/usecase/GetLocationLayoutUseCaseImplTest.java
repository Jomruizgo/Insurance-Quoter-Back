package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLocationLayoutUseCaseImplTest {

    @Mock
    private QuoteLayoutRepository quoteLayoutRepository;

    @InjectMocks
    private GetLocationLayoutUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    // CRITERIO-1.1: returns layout when quote exists with layout configured
    @Test
    void shouldReturnLayout_whenFolioExistsWithLayoutConfigured() {
        // GIVEN
        var data = new QuoteLayoutData(1L, "FOL-2026-00042", 3, "MULTIPLE", 2L, NOW);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(data));

        // WHEN
        GetLayoutResult result = useCase.getLayout("FOL-2026-00042");

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(result.version()).isEqualTo(2L);
        assertThat(result.layoutConfiguration().numberOfLocations()).isEqualTo(3);
        assertThat(result.layoutConfiguration().locationType()).isEqualTo(LocationType.MULTIPLE);
    }

    // CRITERIO-1.3: returns null layout fields when quote has no layout configured
    @Test
    void shouldReturnNullLayout_whenFolioExistsWithoutLayoutConfigured() {
        // GIVEN
        var data = new QuoteLayoutData(1L, "FOL-2026-00042", null, null, 1L, NOW);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(data));

        // WHEN
        GetLayoutResult result = useCase.getLayout("FOL-2026-00042");

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(result.layoutConfiguration().numberOfLocations()).isNull();
        assertThat(result.layoutConfiguration().locationType()).isNull();
    }

    // CRITERIO-1.2: throws FolioNotFoundException when folio does not exist
    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        when(quoteLayoutRepository.findByFolioNumber("FOL-9999-99999")).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getLayout("FOL-9999-99999"))
                .isInstanceOf(FolioNotFoundException.class)
                .hasMessageContaining("FOL-9999-99999");
    }
}
