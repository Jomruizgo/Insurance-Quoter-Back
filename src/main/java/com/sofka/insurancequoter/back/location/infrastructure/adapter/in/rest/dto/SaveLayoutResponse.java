package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

// HTTP response DTO for PUT /v1/quotes/{folio}/locations/layout
public record SaveLayoutResponse(
        String folioNumber,
        LayoutConfigurationDto layoutConfiguration,
        Instant updatedAt,
        Long version
) {
}
