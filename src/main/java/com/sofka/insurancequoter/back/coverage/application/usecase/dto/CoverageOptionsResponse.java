package com.sofka.insurancequoter.back.coverage.application.usecase.dto;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;

import java.time.Instant;
import java.util.List;

// Application-layer response DTO for coverage options queries and writes
public record CoverageOptionsResponse(
        String folioNumber,
        List<CoverageOption> coverageOptions,
        Instant updatedAt,
        long version
) {}
