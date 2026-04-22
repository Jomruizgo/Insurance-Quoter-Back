package com.sofka.insurancequoter.back.folio.domain.model;

import java.time.Instant;

public record QuoteState(
        String folioNumber,
        String quoteStatus,
        int completionPercentage,
        QuoteSections sections,
        Long version,
        Instant updatedAt
) {}
