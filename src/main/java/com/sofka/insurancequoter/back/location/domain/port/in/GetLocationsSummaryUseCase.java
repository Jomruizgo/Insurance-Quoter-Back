package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.domain.model.LocationSummary;

import java.util.List;

// Input port for retrieving a validation summary of all locations in a quote
public interface GetLocationsSummaryUseCase {

    SummaryResult getSummary(String folioNumber);

    record SummaryResult(
            String folioNumber,
            int totalLocations,
            int completeLocations,
            int incompleteLocations,
            List<LocationSummary> locations
    ) {}
}
