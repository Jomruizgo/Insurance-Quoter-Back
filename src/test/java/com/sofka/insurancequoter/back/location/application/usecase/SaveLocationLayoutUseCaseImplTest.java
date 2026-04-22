package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class SaveLocationLayoutUseCaseImplTest {

    @Mock
    private QuoteLayoutRepository quoteLayoutRepository;

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private SaveLocationLayoutUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    // Helper to build minimal Location domain objects for layout tests
    private static Location loc(int index, boolean active) {
        return new Location(index, active, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, ValidationStatus.INCOMPLETE, List.of());
    }

    private QuoteLayoutData quoteData(Long id, Integer currentLocations, String currentType, Long version) {
        return new QuoteLayoutData(id, "FOL-2026-00042", currentLocations, currentType, version, NOW);
    }

    // CRITERIO-2.1: first time save creates the empty location records
    @Test
    void shouldCreateLocations_whenSavingLayoutForFirstTime() {
        // GIVEN
        var existing = quoteData(10L, null, null, 1L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L)).thenReturn(List.of());
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(3, LocationType.MULTIPLE),
                1L
        );

        // WHEN
        SaveLayoutResult result = useCase.saveLayout(command);

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(result.layoutConfiguration().numberOfLocations()).isEqualTo(3);
        assertThat(result.layoutConfiguration().locationType()).isEqualTo(LocationType.MULTIPLE);

        ArgumentCaptor<List<Location>> captor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).insertAll(eq(10L), captor.capture());
        List<Location> inserted = captor.getValue();
        assertThat(inserted).hasSize(3);
        assertThat(inserted).extracting(Location::index).containsExactly(1, 2, 3);
        assertThat(inserted).extracting(Location::active).containsOnly(true);
        verify(locationRepository, never()).reactivateByIndices(any(), any());
    }

    // CRITERIO-2.2: increasing numberOfLocations appends new empty locations (no prior inactive rows)
    @Test
    void shouldAddLocations_whenIncreasingNumberOfLocations() {
        // GIVEN
        var existing = quoteData(10L, 2, "MULTIPLE", 3L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L)).thenReturn(List.of(loc(1, true), loc(2, true)));
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(4, LocationType.MULTIPLE),
                3L
        );

        // WHEN
        useCase.saveLayout(command);

        // THEN — only the 2 new locations (index 3 and 4) are inserted
        ArgumentCaptor<List<Location>> captor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).insertAll(eq(10L), captor.capture());
        List<Location> inserted = captor.getValue();
        assertThat(inserted).hasSize(2);
        assertThat(inserted).extracting(Location::index).containsExactly(3, 4);
        verify(locationRepository, never()).reactivateByIndices(any(), any());
    }

    // CRITERIO-2.3: decreasing numberOfLocations marks excess locations inactive
    @Test
    void shouldDeactivateLocations_whenDecreasingNumberOfLocations() {
        // GIVEN
        var existing = quoteData(10L, 4, "MULTIPLE", 4L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L))
                .thenReturn(List.of(loc(1, true), loc(2, true), loc(3, true), loc(4, true)));
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(2, LocationType.MULTIPLE),
                4L
        );

        // WHEN
        useCase.saveLayout(command);

        // THEN — indices 3 and 4 are deactivated, no inserts
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).deactivateByIndices(eq(10L), captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(3, 4);
        verify(locationRepository, never()).insertAll(any(), any());
        verify(locationRepository, never()).reactivateByIndices(any(), any());
    }

    // R-005: reduce then increase must reactivate existing inactive rows, not insert duplicates
    @Test
    void shouldReactivateInactiveLocations_whenIncreasingAfterPreviousReduction() {
        // GIVEN: previously had 4, reduced to 2 (indices 3,4 now inactive)
        var existing = quoteData(10L, 2, "MULTIPLE", 5L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L))
                .thenReturn(List.of(loc(1, true), loc(2, true), loc(3, false), loc(4, false)));
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(4, LocationType.MULTIPLE),
                5L
        );

        // WHEN
        useCase.saveLayout(command);

        // THEN — reactivate 3 and 4, no inserts (they already exist)
        ArgumentCaptor<List<Integer>> reactivateCaptor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).reactivateByIndices(eq(10L), reactivateCaptor.capture());
        assertThat(reactivateCaptor.getValue()).containsExactlyInAnyOrder(3, 4);
        verify(locationRepository, never()).insertAll(any(), any());
        verify(locationRepository, never()).deactivateByIndices(any(), any());
    }

    // Increase beyond max ever-allocated: reactivate existing inactive + insert truly new
    @Test
    void shouldReactivateAndInsert_whenIncreasingBeyondMaxExistingIndex() {
        // GIVEN: previously had 3, reduced to 1 (indices 2,3 inactive), now requesting 5
        var existing = quoteData(10L, 1, "MULTIPLE", 6L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L))
                .thenReturn(List.of(loc(1, true), loc(2, false), loc(3, false)));
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(5, LocationType.MULTIPLE),
                6L
        );

        // WHEN
        useCase.saveLayout(command);

        // THEN — reactivate 2,3 and insert 4,5
        ArgumentCaptor<List<Integer>> reactivateCaptor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).reactivateByIndices(eq(10L), reactivateCaptor.capture());
        assertThat(reactivateCaptor.getValue()).containsExactlyInAnyOrder(2, 3);

        ArgumentCaptor<List<Location>> insertCaptor = ArgumentCaptor.forClass(List.class);
        verify(locationRepository).insertAll(eq(10L), insertCaptor.capture());
        assertThat(insertCaptor.getValue()).extracting(Location::index).containsExactly(4, 5);
    }

    // Equal numberOfLocations: no location changes, only quote fields updated
    @Test
    void shouldNotTouchLocations_whenNumberOfLocationsIsUnchanged() {
        // GIVEN
        var existing = quoteData(10L, 3, "MULTIPLE", 2L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));
        when(locationRepository.findAllByQuoteId(10L))
                .thenReturn(List.of(loc(1, true), loc(2, true), loc(3, true)));
        when(quoteLayoutRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(3, LocationType.MULTIPLE),
                2L
        );

        // WHEN
        useCase.saveLayout(command);

        // THEN — no location writes
        verify(locationRepository, never()).insertAll(any(), any());
        verify(locationRepository, never()).deactivateByIndices(any(), any());
        verify(locationRepository, never()).reactivateByIndices(any(), any());
    }

    // CRITERIO-2.5: folio not found throws FolioNotFoundException
    @Test
    void shouldThrowFolioNotFoundException_whenFolioDoesNotExist() {
        // GIVEN
        when(quoteLayoutRepository.findByFolioNumber("FOL-9999-99999")).thenReturn(Optional.empty());

        var command = new SaveLayoutCommand(
                "FOL-9999-99999",
                new LayoutConfiguration(2, LocationType.MULTIPLE),
                1L
        );

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveLayout(command))
                .isInstanceOf(FolioNotFoundException.class)
                .hasMessageContaining("FOL-9999-99999");
    }

    // CRITERIO-2.6 / business rule: SINGLE type must have numberOfLocations == 1
    @Test
    void shouldThrowIllegalArgument_whenSingleTypeHasMoreThanOneLocation() {
        // GIVEN
        var existing = quoteData(10L, null, null, 1L);
        when(quoteLayoutRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(existing));

        var command = new SaveLayoutCommand(
                "FOL-2026-00042",
                new LayoutConfiguration(2, LocationType.SINGLE),
                1L
        );

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.saveLayout(command))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
