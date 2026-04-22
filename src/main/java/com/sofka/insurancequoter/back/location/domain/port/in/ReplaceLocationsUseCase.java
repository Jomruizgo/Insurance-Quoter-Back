package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.time.Instant;
import java.util.List;

// Input port for replacing all locations of a quote in one operation
public interface ReplaceLocationsUseCase {

    ReplaceLocationsResult replaceLocations(ReplaceLocationsCommand command);

    record ReplaceLocationsCommand(String folioNumber, List<LocationData> locations, Long version) {}

    record LocationData(
            int index,
            String locationName,
            String address,
            String zipCode,
            String constructionType,
            Integer level,
            Integer constructionYear,
            com.sofka.insurancequoter.back.location.domain.model.BusinessLine businessLine,
            java.util.List<com.sofka.insurancequoter.back.location.domain.model.Guarantee> guarantees
    ) {}

    record ReplaceLocationsResult(
            String folioNumber,
            List<Location> locations,
            Instant updatedAt,
            Long version
    ) {}
}
