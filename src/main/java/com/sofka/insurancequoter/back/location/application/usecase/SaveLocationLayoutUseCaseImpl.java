package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.domain.port.in.SaveLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// Saves or updates the location layout for a quote and synchronises the location records
public class SaveLocationLayoutUseCaseImpl implements SaveLocationLayoutUseCase {

    private final QuoteLayoutRepository quoteLayoutRepository;
    private final LocationRepository locationRepository;

    public SaveLocationLayoutUseCaseImpl(
            QuoteLayoutRepository quoteLayoutRepository,
            LocationRepository locationRepository) {
        this.quoteLayoutRepository = quoteLayoutRepository;
        this.locationRepository = locationRepository;
    }

    @Override
    @Transactional
    public SaveLayoutResult saveLayout(SaveLayoutCommand command) {
        QuoteLayoutData existing = quoteLayoutRepository.findByFolioNumber(command.folioNumber())
                .orElseThrow(() -> new FolioNotFoundException(command.folioNumber()));

        LayoutConfiguration layout = command.layoutConfiguration();

        if (LocationType.SINGLE.equals(layout.locationType())
                && !Integer.valueOf(1).equals(layout.numberOfLocations())) {
            throw new IllegalArgumentException(
                    "numberOfLocations must be 1 when locationType is SINGLE");
        }

        int requested = layout.numberOfLocations();

        // Load ALL locations (active + inactive) to know what indices already exist in DB.
        // Using only active count would cause UK constraint violations when re-increasing
        // after a previous reduction (inactive rows with those indices already exist).
        List<Location> allLocations = locationRepository.findAllByQuoteId(existing.id());
        int maxExistingIndex = allLocations.stream().mapToInt(Location::index).max().orElse(0);
        int currentActive = (int) allLocations.stream().filter(Location::active).count();

        if (requested > currentActive) {
            // Reactivate any previously deactivated rows whose index falls within the new range
            List<Integer> indicesToReactivate = allLocations.stream()
                    .filter(l -> !l.active() && l.index() <= requested)
                    .map(Location::index)
                    .toList();
            if (!indicesToReactivate.isEmpty()) {
                locationRepository.reactivateByIndices(existing.id(), indicesToReactivate);
            }

            // Insert truly new rows for indices beyond what has ever been allocated
            if (requested > maxExistingIndex) {
                List<Location> newLocations = new ArrayList<>();
                for (int i = maxExistingIndex + 1; i <= requested; i++) {
                    newLocations.add(new Location(i, true, null, null, null,
                            null, null, null, null, null, null, null,
                            null, null, null,
                            com.sofka.insurancequoter.back.location.domain.model.ValidationStatus.INCOMPLETE,
                            java.util.List.of()));
                }
                locationRepository.insertAll(existing.id(), newLocations);
            }

        } else if (requested < currentActive) {
            List<Integer> indicesToDeactivate = allLocations.stream()
                    .filter(l -> l.active() && l.index() > requested)
                    .map(Location::index)
                    .toList();
            locationRepository.deactivateByIndices(existing.id(), indicesToDeactivate);
        }
        // equal — no location changes needed

        String locationTypeName = layout.locationType() != null ? layout.locationType().name() : null;
        QuoteLayoutData updated = new QuoteLayoutData(
                existing.id(),
                existing.folioNumber(),
                layout.numberOfLocations(),
                locationTypeName,
                command.version(),
                existing.updatedAt()
        );

        QuoteLayoutData saved = quoteLayoutRepository.save(updated);

        return new SaveLayoutResult(
                saved.folioNumber(),
                new LayoutConfiguration(saved.numberOfLocations(),
                        saved.locationType() != null ? LocationType.valueOf(saved.locationType()) : null),
                saved.updatedAt(),
                saved.version()
        );
    }
}
