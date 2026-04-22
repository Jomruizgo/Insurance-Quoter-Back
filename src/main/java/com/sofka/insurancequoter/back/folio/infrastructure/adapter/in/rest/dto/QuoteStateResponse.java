package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

public record QuoteStateResponse(
        String folioNumber,
        String quoteStatus,
        int completionPercentage,
        SectionsResponse sections,
        Long version,
        Instant updatedAt
) {}
