package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Response DTO for POST /v1/quotes/{folio}/calculate
public record CalculationResponse(
        String folioNumber,
        String quoteStatus,
        BigDecimal netPremium,
        BigDecimal commercialPremium,
        List<PremiumByLocationResponse> premiumsByLocation,
        Instant calculatedAt,
        long version
) {}
