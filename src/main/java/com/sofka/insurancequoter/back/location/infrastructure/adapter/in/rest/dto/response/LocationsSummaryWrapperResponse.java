package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

// Response for GET /v1/quotes/{folio}/locations/summary
public record LocationsSummaryWrapperResponse(
        String folioNumber,
        int totalLocations,
        int completeLocations,
        int incompleteLocations,
        List<LocationSummaryItemResponse> locations
) {
}
