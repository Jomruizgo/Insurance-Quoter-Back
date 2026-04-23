package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

// Request body for POST /v1/quotes/{folio}/calculate
public record CalculatePremiumRequest(@NotNull Long version) {}
