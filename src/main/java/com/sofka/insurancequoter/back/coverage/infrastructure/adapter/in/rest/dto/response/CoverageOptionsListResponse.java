package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;
import java.util.List;

// HTTP response body for GET and PUT /v1/quotes/{folio}/coverage-options
public record CoverageOptionsListResponse(
        String folioNumber,
        List<CoverageOptionItemResponse> coverageOptions,
        Instant updatedAt,
        long version
) {}
