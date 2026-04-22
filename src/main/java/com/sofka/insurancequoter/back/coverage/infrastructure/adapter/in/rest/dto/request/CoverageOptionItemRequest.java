package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

// Request item for a single coverage option within SaveCoverageOptionsRequest
public record CoverageOptionItemRequest(
        @NotBlank String code,
        boolean selected,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal deductiblePercentage,
        @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal coinsurancePercentage
) {}
