package com.sofka.insurancequoter.back.calculation.application.usecase.dto;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.util.List;

// Snapshot of a quote's data needed to perform premium calculation
public record QuoteCalculationSnapshot(
        String folioNumber,
        long version,
        List<Location> locations,
        List<CoverageOption> coverageOptions
) {}
