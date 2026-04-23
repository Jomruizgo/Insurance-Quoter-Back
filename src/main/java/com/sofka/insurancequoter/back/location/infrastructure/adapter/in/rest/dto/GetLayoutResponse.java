package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto;

// HTTP response DTO for GET /v1/quotes/{folio}/locations/layout
public record GetLayoutResponse(
        String folioNumber,
        LayoutConfigurationDto layoutConfiguration,
        Long version
) {
}
