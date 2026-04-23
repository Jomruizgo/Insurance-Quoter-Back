package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

// Response for GET /v1/quotes/{folio}/locations
public record LocationsListResponse(
        String folioNumber,
        List<LocationDetailResponse> locations,
        Long version
) {
}
