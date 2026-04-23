package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.PatchLocationUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.ZipCodeValidationClient;
import com.sofka.insurancequoter.back.location.domain.service.LocationValidationService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Partially updates a single location, recalculates alerts, and increments the quote version
public class PatchLocationUseCaseImpl implements PatchLocationUseCase {

    private final LocationRepository locationRepository;
    private final QuoteVersionRepository quoteVersionRepository;
    private final ZipCodeValidationClient zipCodeValidationClient;
    private final LocationValidationService locationValidationService;

    public PatchLocationUseCaseImpl(LocationRepository locationRepository,
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
    public PatchLocationResult patchLocation(PatchLocationCommand command) {
        Long storedVersion = quoteVersionRepository.findVersionByFolioNumber(command.folioNumber());
        if (!storedVersion.equals(command.version())) {
            throw new VersionConflictException(command.folioNumber(), command.version(), storedVersion);
        }

        if (!locationRepository.existsByFolioAndIndex(command.folioNumber(), command.index())) {
            throw new LocationNotFoundException(command.folioNumber(), command.index());
        }

        Location existing = locationRepository.findByFolioNumber(command.folioNumber())
                .stream()
                .filter(l -> l.index() == command.index())
                .findFirst()
                .orElseThrow(() -> new LocationNotFoundException(command.folioNumber(), command.index()));

        // Apply partial merge — only overwrite fields that were explicitly provided
        String locationName = command.locationName().orElse(existing.locationName());
        String address = command.address().orElse(existing.address());
        String zipCode = command.zipCode().orElse(existing.zipCode());
        String constructionType = command.constructionType().orElse(existing.constructionType());
        Integer level = command.level().orElse(existing.level());
        Integer constructionYear = command.constructionYear().orElse(existing.constructionYear());
        BusinessLine businessLine = command.businessLine().orElse(existing.businessLine());
        List<Guarantee> guarantees = command.guarantees().orElse(existing.guarantees());

        Optional<ZipCodeInfo> zipInfo = zipCode != null && !zipCode.isBlank()
                ? zipCodeValidationClient.validate(zipCode)
                : Optional.empty();

        Location merged = new Location(
                existing.index(), existing.active(), locationName, address, zipCode,
                zipInfo.map(ZipCodeInfo::state).orElse(existing.state()),
                zipInfo.map(ZipCodeInfo::municipality).orElse(existing.municipality()),
                existing.neighborhood(),
                zipInfo.map(ZipCodeInfo::city).orElse(existing.city()),
                constructionType, level, constructionYear,
                businessLine, guarantees,
                zipInfo.map(ZipCodeInfo::catastrophicZone).orElse(existing.catastrophicZone()),
                ValidationStatus.INCOMPLETE, List.of()
        );

        List<BlockingAlert> alerts = locationValidationService.calculateAlerts(merged, zipInfo);
        ValidationStatus status = locationValidationService.deriveStatus(alerts);

        Location withStatus = new Location(
                merged.index(), merged.active(), merged.locationName(), merged.address(), merged.zipCode(),
                merged.state(), merged.municipality(), merged.neighborhood(), merged.city(),
                merged.constructionType(), merged.level(), merged.constructionYear(),
                merged.businessLine(), merged.guarantees(), merged.catastrophicZone(),
                status, alerts
        );

        Location saved = locationRepository.patchOne(command.folioNumber(), command.index(), withStatus);
        quoteVersionRepository.incrementVersion(command.folioNumber());

        return new PatchLocationResult(
                command.folioNumber(),
                saved,
                quoteVersionRepository.getUpdatedAt(command.folioNumber()),
                storedVersion + 1
        );
    }
}
