package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.PatchLocationUseCase;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatchLocationUseCaseImplTest {

    @Mock private LocationRepository locationRepository;
    @Mock private QuoteVersionRepository quoteVersionRepository;
    @Mock private ZipCodeValidationClient zipCodeValidationClient;
    @Mock private LocationValidationService locationValidationService;

    @InjectMocks
    private PatchLocationUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    private Location baseLocation() {
        return new Location(1, true, "Bodega Original", "Av. Test 100", "06600",
                "CDMX", "Cuauhtemoc", null, "CDMX", "MASONRY", 2, 1990,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))),
                "ZONE_A", ValidationStatus.COMPLETE, List.of());
    }

    @Test
    void patchLocation_indexNotFound_throwsLocationNotFoundException() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        when(locationRepository.existsByFolioAndIndex("FOL-001", 99)).thenReturn(false);

        var command = new PatchLocationUseCase.PatchLocationCommand(
                "FOL-001", 99, 4L,
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.patchLocation(command))
                .isInstanceOf(LocationNotFoundException.class);
    }

    @Test
    void patchLocation_versionConflict_throwsVersionConflictException() {
        // GIVEN
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(5L);

        var command = new PatchLocationUseCase.PatchLocationCommand(
                "FOL-001", 1, 3L, // wrong version
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.patchLocation(command))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void patchLocation_partialPatch_onlySentFieldsChange() {
        // GIVEN
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        when(locationRepository.existsByFolioAndIndex("FOL-001", 1)).thenReturn(true);
        when(locationRepository.findByFolioNumber("FOL-001")).thenReturn(List.of(baseLocation()));
        when(zipCodeValidationClient.validate("06600")).thenReturn(Optional.of(zipInfo));
        when(locationValidationService.calculateAlerts(any(), any())).thenReturn(List.of());
        when(locationValidationService.deriveStatus(List.of())).thenReturn(ValidationStatus.COMPLETE);

        Location patched = new Location(1, true, "Bodega Nueva", "Av. Test 100", "06600",
                "CDMX", "Cuauhtemoc", null, "CDMX", "MASONRY", 2, 1990,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))),
                "ZONE_A", ValidationStatus.COMPLETE, List.of());
        when(locationRepository.patchOne(any(), anyInt(), any())).thenReturn(patched);
        when(quoteVersionRepository.getUpdatedAt("FOL-001")).thenReturn(NOW);

        var command = new PatchLocationUseCase.PatchLocationCommand(
                "FOL-001", 1, 4L,
                Optional.of("Bodega Nueva"), // only name changes
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        // WHEN
        PatchLocationUseCase.PatchLocationResult result = useCase.patchLocation(command);

        // THEN
        assertThat(result.location().locationName()).isEqualTo("Bodega Nueva");
        assertThat(result.location().address()).isEqualTo("Av. Test 100"); // unchanged
        verify(quoteVersionRepository).incrementVersion("FOL-001");
    }

    @Test
    void patchLocation_withValidZipCode_removesMissingZipCodeAlert() {
        // GIVEN
        ZipCodeInfo zipInfo = new ZipCodeInfo("44100", "Jalisco", "Guadalajara", "Guadalajara", "ZONE_B", true);
        Location original = new Location(1, true, "Sucursal", null, null,
                null, null, null, null, null, null, null,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(500_000))),
                null, ValidationStatus.INCOMPLETE,
                List.of(new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "CP requerido")));

        when(quoteVersionRepository.findVersionByFolioNumber("FOL-001")).thenReturn(4L);
        when(locationRepository.existsByFolioAndIndex("FOL-001", 1)).thenReturn(true);
        when(locationRepository.findByFolioNumber("FOL-001")).thenReturn(List.of(original));
        when(zipCodeValidationClient.validate("44100")).thenReturn(Optional.of(zipInfo));
        when(locationValidationService.calculateAlerts(any(), any())).thenReturn(List.of());
        when(locationValidationService.deriveStatus(List.of())).thenReturn(ValidationStatus.COMPLETE);

        Location fixed = new Location(1, true, "Sucursal", null, "44100",
                "Jalisco", "Guadalajara", null, "Guadalajara", null, null, null,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(500_000))),
                "ZONE_B", ValidationStatus.COMPLETE, List.of());
        when(locationRepository.patchOne(any(), anyInt(), any())).thenReturn(fixed);
        when(quoteVersionRepository.getUpdatedAt("FOL-001")).thenReturn(NOW);

        var command = new PatchLocationUseCase.PatchLocationCommand(
                "FOL-001", 1, 4L,
                Optional.empty(), Optional.empty(), Optional.of("44100"),
                Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty());

        // WHEN
        PatchLocationUseCase.PatchLocationResult result = useCase.patchLocation(command);

        // THEN
        assertThat(result.location().validationStatus()).isEqualTo(ValidationStatus.COMPLETE);
        assertThat(result.location().blockingAlerts()).isEmpty();
    }
}
