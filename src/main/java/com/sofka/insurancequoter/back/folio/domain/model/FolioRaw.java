package com.sofka.insurancequoter.back.folio.domain.model;

import java.time.Instant;

// Intermediate domain object: raw folio data from persistence, without enrichment.
// agentName and completionPct are resolved by the use case layer.
public record FolioRaw(
        String folioNumber,
        String client,
        String agentCode,
        String status,
        int locationCount,
        Instant updatedAt
) {}
