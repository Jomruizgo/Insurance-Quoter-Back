package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// HTTP request DTO for PUT /v1/quotes/{folio}/locations/layout
public record SaveLayoutRequest(
        @NotNull @Valid LayoutConfigurationDto layoutConfiguration,
        @NotNull Long version
) {
}
