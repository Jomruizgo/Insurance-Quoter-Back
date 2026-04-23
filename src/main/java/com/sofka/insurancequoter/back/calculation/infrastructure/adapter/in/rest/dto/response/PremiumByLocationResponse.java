package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response;

import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;

import java.math.BigDecimal;
import java.util.List;

// Response DTO for a single location premium entry
public record PremiumByLocationResponse(
        int index,
        String locationName,
        BigDecimal netPremium,
        BigDecimal commercialPremium,
        boolean calculable,
        CoverageBreakdownResponse coverageBreakdown,
        List<BlockingAlert> blockingAlerts
) {}
