package com.sofka.insurancequoter.back.folio.domain.model;

import java.time.Instant;

// Read-only projection of Quote aggregate used for state evaluation
public record QuoteSnapshot(
        String folioNumber,
        String quoteStatus,
        Integer numberOfLocations,
        String locationType,
        Long version,
        Instant updatedAt,
        boolean hasGeneralInfo
) {}
