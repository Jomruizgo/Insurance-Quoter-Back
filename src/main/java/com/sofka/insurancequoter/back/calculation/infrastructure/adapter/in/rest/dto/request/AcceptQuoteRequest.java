package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request body for POST /v1/quotes/{folio}/accept
public record AcceptQuoteRequest(
        @NotBlank String acceptedBy,
        @NotNull Long version
) {}
