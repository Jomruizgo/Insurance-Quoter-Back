package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;

// Response item for a single coverage option in CoverageOptionsListResponse
public record CoverageOptionItemResponse(
        String code,
        String description,
        boolean selected,
        BigDecimal deductiblePercentage,
        BigDecimal coinsurancePercentage
) {}
