package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// Request body for PUT /v1/quotes/{folio}/coverage-options
public record SaveCoverageOptionsRequest(
        @NotNull @Valid List<CoverageOptionItemRequest> coverageOptions,
        @NotNull Long version
) {}
