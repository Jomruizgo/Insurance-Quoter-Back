package com.sofka.insurancequoter.back.calculation.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// Aggregate root for a premium calculation result tied to a quote folio
public record CalculationResult(
        String folioNumber,
        BigDecimal netPremium,
        BigDecimal commercialPremium,
        List<PremiumByLocation> premiumsByLocation,
        Instant calculatedAt,
        long version    // quote version after persist (0 before persisting)
) {}
