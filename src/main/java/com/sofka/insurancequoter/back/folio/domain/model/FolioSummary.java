package com.sofka.insurancequoter.back.folio.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

// Domain model for the folio list response — all 9 fields from the API contract.
// commercialPremium is null until the quote reaches CALCULATED status.
public record FolioSummary(
        String folioNumber,
        String client,
        String agentCode,
        String agentName,
        String status,
        int locationCount,
        int completionPct,
        BigDecimal commercialPremium,
        Instant updatedAt
) {}
