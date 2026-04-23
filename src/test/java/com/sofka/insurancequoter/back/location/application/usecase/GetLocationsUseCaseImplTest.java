package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationsUseCase;
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
class GetLocationsUseCaseImplTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private QuoteVersionRepository quoteVersionRepository;

    @InjectMocks
    private GetLocationsUseCaseImpl useCase;

    private static Location loc(int index) {
        return new Location(index, true, "Loc " + index, null, null,
                null, null, null, null, null, null, null,
                null, null, null, ValidationStatus.INCOMPLETE, List.of());
    }

    @Test
    void getLocations_returnsLocationsAndVersion_whenFolioExists() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(3L);
        when(locationRepository.findByFolioNumber("FOL-001")).thenReturn(List.of(loc(1), loc(2)));

        // WHEN
        GetLocationsUseCase.LocationsResult result = useCase.getLocations("FOL-001");

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-001");
        assertThat(result.locations()).hasSize(2);
        assertThat(result.version()).isEqualTo(3L);
    }

    @Test
    void getLocations_throwsFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("UNKNOWN"))
                .thenThrow(new FolioNotFoundException("UNKNOWN"));

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getLocations("UNKNOWN"))
                .isInstanceOf(FolioNotFoundException.class);
    }
}
