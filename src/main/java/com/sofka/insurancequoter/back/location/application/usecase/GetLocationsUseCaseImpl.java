package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationsUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;

// Retrieves all active locations and the current version for a given folio
public class GetLocationsUseCaseImpl implements GetLocationsUseCase {

    private final LocationRepository locationRepository;
    private final QuoteVersionRepository quoteVersionRepository;

    public GetLocationsUseCaseImpl(LocationRepository locationRepository,
                                   QuoteVersionRepository quoteVersionRepository) {
        this.locationRepository = locationRepository;
        this.quoteVersionRepository = quoteVersionRepository;
    }

    @Override
    public LocationsResult getLocations(String folioNumber) {
        Long version = quoteVersionRepository.findVersionByFolioNumber(folioNumber);
        var locations = locationRepository.findByFolioNumber(folioNumber);
        return new LocationsResult(folioNumber, locations, version);
    }
}
