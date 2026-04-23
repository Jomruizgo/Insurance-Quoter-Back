package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
import com.sofka.insurancequoter.back.location.domain.model.LocationSummary;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationsSummaryUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetLocationsSummaryUseCaseImplTest {

    @Mock private LocationRepository locationRepository;
    @Mock private QuoteVersionRepository quoteVersionRepository;

    @InjectMocks
    private GetLocationsSummaryUseCaseImpl useCase;

    @Test
    void getSummary_returnsCorrectCounts_whenFolioExists() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        List<LocationSummary> summaries = List.of(
                new LocationSummary(1, "Bodega", ValidationStatus.COMPLETE, List.of()),
                new LocationSummary(2, "Oficina", ValidationStatus.INCOMPLETE,
                        List.of(new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "CP requerido"))),
                new LocationSummary(3, "Almacen", ValidationStatus.COMPLETE, List.of())
        );
        when(locationRepository.findSummaryByFolioNumber("FOL-001")).thenReturn(summaries);

        // WHEN
        GetLocationsSummaryUseCase.SummaryResult result = useCase.getSummary("FOL-001");

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-001");
        assertThat(result.totalLocations()).isEqualTo(3);
        assertThat(result.completeLocations()).isEqualTo(2);
        assertThat(result.incompleteLocations()).isEqualTo(1);
        assertThat(result.locations()).hasSize(3);
    }

    @Test
    void getSummary_throwsFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("UNKNOWN"))
                .thenThrow(new FolioNotFoundException("UNKNOWN"));

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getSummary("UNKNOWN"))
                .isInstanceOf(FolioNotFoundException.class);
    }
}
