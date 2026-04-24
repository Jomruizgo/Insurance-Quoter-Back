package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

// Response DTO for a single folio entry in GET /v1/folios
public record FolioSummaryDto(
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
