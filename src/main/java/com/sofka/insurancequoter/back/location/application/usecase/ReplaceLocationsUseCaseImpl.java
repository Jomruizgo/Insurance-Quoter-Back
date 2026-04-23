package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.ReplaceLocationsUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.ZipCodeValidationClient;
import com.sofka.insurancequoter.back.location.domain.service.LocationValidationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Replaces all locations of a quote and recalculates blocking alerts for each one
public class ReplaceLocationsUseCaseImpl implements ReplaceLocationsUseCase {

    private final LocationRepository locationRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final ZipCodeValidationClient zipCodeValidationClient;
    private final LocationValidationService locationValidationService;

    public ReplaceLocationsUseCaseImpl(LocationRepository locationRepository,
                                       QuoteVersionRepository quoteVersionRepository,
                                       ZipCodeValidationClient zipCodeValidationClient,
                                       LocationValidationService locationValidationService) {
        this.locationRepository = locationRepository;
        this.quoteVersionRepository = quoteVersionRepository;
        this.zipCodeValidationClient = zipCodeValidationClient;
        this.locationValidationService = locationValidationService;
    }

    @Override
    @Transactional
    public ReplaceLocationsResult replaceLocations(ReplaceLocationsCommand command) {
        Long storedVersion = quoteVersionRepository.findVersionByFolioNumber(command.folioNumber());
        if (!storedVersion.equals(command.version())) {
            throw new VersionConflictException(command.folioNumber(), command.version(), storedVersion);
        }

        List<Location> enriched = command.locations().stream()
                .map(data -> enrich(data, command.folioNumber()))
                .toList();

        List<Location> saved = locationRepository.replaceAll(command.folioNumber(), enriched);
        quoteVersionRepository.incrementVersion(command.folioNumber());

        return new ReplaceLocationsResult(
                command.folioNumber(),
                saved,
                quoteVersionRepository.getUpdatedAt(command.folioNumber()),
                storedVersion + 1
        );
    }

    private Location enrich(LocationData data, String folioNumber) {
        Optional<ZipCodeInfo> zipInfo = data.zipCode() != null && !data.zipCode().isBlank()
                ? zipCodeValidationClient.validate(data.zipCode())
                : Optional.empty();

        // Build preliminary location without computed fields to pass into validation service
        Location preliminary = new Location(
                data.index(), true, data.locationName(), data.address(), data.zipCode(),
                zipInfo.map(ZipCodeInfo::state).orElse(null),
                zipInfo.map(ZipCodeInfo::municipality).orElse(null),
                null, // neighborhood not provided by core zip endpoint
                zipInfo.map(ZipCodeInfo::city).orElse(null),
                data.constructionType(), data.level(), data.constructionYear(),
                data.businessLine(), data.guarantees(),
                zipInfo.map(ZipCodeInfo::catastrophicZone).orElse(null),
                ValidationStatus.INCOMPLETE, List.of()
        );

        List<BlockingAlert> alerts = locationValidationService.calculateAlerts(preliminary, zipInfo);
        ValidationStatus status = locationValidationService.deriveStatus(alerts);

        return new Location(
                data.index(), true, data.locationName(), data.address(), data.zipCode(),
                zipInfo.map(ZipCodeInfo::state).orElse(null),
                zipInfo.map(ZipCodeInfo::municipality).orElse(null),
                null,
                zipInfo.map(ZipCodeInfo::city).orElse(null),
                data.constructionType(), data.level(), data.constructionYear(),
                data.businessLine(), data.guarantees(),
                zipInfo.map(ZipCodeInfo::catastrophicZone).orElse(null),
                status, alerts
        );
    }
}
