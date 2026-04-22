package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.List;

// Response for PUT /v1/quotes/{folio}/locations (includes updatedAt)
public record LocationsListResponseWithTimestamp(
        String folioNumber,
        List<LocationDetailResponse> locations,
        Instant updatedAt,
        Long version
) {
}
