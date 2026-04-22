package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.util.List;

// Input port for retrieving all locations of a quote
public interface GetLocationsUseCase {

    // Returns all active locations and the current version for the given folio
    LocationsResult getLocations(String folioNumber);

    record LocationsResult(String folioNumber, List<Location> locations, Long version) {}
}
