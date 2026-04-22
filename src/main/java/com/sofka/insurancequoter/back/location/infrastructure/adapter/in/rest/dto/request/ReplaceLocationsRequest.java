package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ReplaceLocationsRequest(
        @NotNull List<LocationItemRequest> locations,
        @NotNull Long version
) {
}
