package com.sofka.insurancequoter.back.folio.domain.model;

import java.time.Instant;

// Domain aggregate root — no JPA or Spring annotations allowed here
public record Quote(
        String folioNumber,
        QuoteStatus quoteStatus,
        String subscriberId,
        String agentCode,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {}
