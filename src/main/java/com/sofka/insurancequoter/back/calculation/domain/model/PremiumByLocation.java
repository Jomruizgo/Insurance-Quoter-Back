package com.sofka.insurancequoter.back.calculation.domain.model;

import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;

import java.math.BigDecimal;
import java.util.List;

// Value object representing the premium calculation result for a single location
public record PremiumByLocation(
        int index,
        String locationName,
        BigDecimal netPremium,          // null if not calculable
        BigDecimal commercialPremium,   // null if not calculable
        boolean calculable,
        CoverageBreakdown coverageBreakdown,  // null if not calculable
        List<BlockingAlert> blockingAlerts
) {}
