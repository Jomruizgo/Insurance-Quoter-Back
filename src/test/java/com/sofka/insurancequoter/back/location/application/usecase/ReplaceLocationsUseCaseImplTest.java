package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.ReplaceLocationsUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.ZipCodeValidationClient;
import com.sofka.insurancequoter.back.location.domain.service.LocationValidationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReplaceLocationsUseCaseImplTest {

    @Mock private LocationRepository locationRepository;
    @Mock private QuoteVersionRepository quoteVersionRepository;
    @Mock private ZipCodeValidationClient zipCodeValidationClient;
    @Mock private LocationValidationService locationValidationService;

    @InjectMocks
    private ReplaceLocationsUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    private ReplaceLocationsUseCase.LocationData validLocationData(int index) {
        return new ReplaceLocationsUseCase.LocationData(
                index, "Bodega " + index, "Av. Test 100", "06600",
                "MASONRY", 2, 1990,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)))
        );
    }

    private Location savedLocation(int index) {
        return new Location(index, true, "Bodega " + index, "Av. Test 100", "06600",
                "CDMX", "Cuauhtemoc", null, "CDMX", "MASONRY", 2, 1990,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))),
                "ZONE_A", ValidationStatus.COMPLETE, List.of());
    }

    @Test
    void replaceLocations_versionConflict_throwsVersionConflictException() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(5L);
        var command = new ReplaceLocationsUseCase.ReplaceLocationsCommand(
                "FOL-001", List.of(validLocationData(1)), 3L); // wrong version

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.replaceLocations(command))
                .isInstanceOf(VersionConflictException.class);
        verify(locationRepository, never()).replaceAll(anyString(), any());
    }

    @Test
    void replaceLocations_validData_returnsCompleteStatus() {
        // GIVEN
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        when(zipCodeValidationClient.validate("06600")).thenReturn(Optional.of(zipInfo));
        when(locationValidationService.calculateAlerts(any(), any())).thenReturn(List.of());
        when(locationValidationService.deriveStatus(List.of())).thenReturn(ValidationStatus.COMPLETE);
        when(locationRepository.replaceAll(anyString(), any())).thenReturn(List.of(savedLocation(1)));
        when(quoteVersionRepository.getUpdatedAt("FOL-001")).thenReturn(NOW);

        var command = new ReplaceLocationsUseCase.ReplaceLocationsCommand(
                "FOL-001", List.of(validLocationData(1)), 4L);

        // WHEN
        ReplaceLocationsUseCase.ReplaceLocationsResult result = useCase.replaceLocations(command);

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-001");
        assertThat(result.locations()).hasSize(1);
        assertThat(result.locations().get(0).validationStatus()).isEqualTo(ValidationStatus.COMPLETE);
        verify(quoteVersionRepository).incrementVersion("FOL-001");
    }

    @Test
    void replaceLocations_invalidZipCode_generates_MISSING_ZIP_CODE_alert() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        when(zipCodeValidationClient.validate("99999")).thenReturn(Optional.empty());
        BlockingAlert alert = new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "CP requerido");
        when(locationValidationService.calculateAlerts(any(), any())).thenReturn(List.of(alert));
        when(locationValidationService.deriveStatus(List.of(alert))).thenReturn(ValidationStatus.INCOMPLETE);

        Location savedWithAlert = new Location(1, true, "Bodega", null, "99999",
                null, null, null, null, null, null, null,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))),
                null, ValidationStatus.INCOMPLETE, List.of(alert));
        when(locationRepository.replaceAll(anyString(), any())).thenReturn(List.of(savedWithAlert));
        when(quoteVersionRepository.getUpdatedAt("FOL-001")).thenReturn(NOW);

        var data = new ReplaceLocationsUseCase.LocationData(
                1, "Bodega", null, "99999", null, null, null,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        var command = new ReplaceLocationsUseCase.ReplaceLocationsCommand("FOL-001", List.of(data), 4L);

        // WHEN
        ReplaceLocationsUseCase.ReplaceLocationsResult result = useCase.replaceLocations(command);

        // THEN
        assertThat(result.locations().get(0).validationStatus()).isEqualTo(ValidationStatus.INCOMPLETE);
        assertThat(result.locations().get(0).blockingAlerts())
                .anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_ZIP_CODE.name()));
    }
}
