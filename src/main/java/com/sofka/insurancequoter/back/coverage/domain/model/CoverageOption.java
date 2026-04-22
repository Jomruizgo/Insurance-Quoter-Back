package com.sofka.insurancequoter.back.coverage.domain.model;

import java.math.BigDecimal;

// Domain value object — no JPA or Spring annotations
public record CoverageOption(
        String code,
        String description,
        boolean selected,
        BigDecimal deductiblePercentage,
        BigDecimal coinsurancePercentage
) {}
