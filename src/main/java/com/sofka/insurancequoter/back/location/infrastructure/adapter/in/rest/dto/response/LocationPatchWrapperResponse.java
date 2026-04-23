package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;

// Response for PATCH /v1/quotes/{folio}/locations/{index}
public record LocationPatchWrapperResponse(
        String folioNumber,
        LocationDetailResponse location,
        Instant updatedAt,
        Long version
) {
}
