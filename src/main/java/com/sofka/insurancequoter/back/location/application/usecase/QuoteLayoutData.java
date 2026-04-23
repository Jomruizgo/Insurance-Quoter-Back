package com.sofka.insurancequoter.back.location.application.usecase;

import java.time.Instant;

// Data transfer object between the persistence output port and the application layer
public record QuoteLayoutData(
        Long id,
        String folioNumber,
        Integer numberOfLocations,
        String locationType,
        Long version,
        Instant updatedAt
) {
}
