package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;

import java.time.Instant;

// Result returned by SaveLocationLayoutUseCase
public record SaveLayoutResult(
        String folioNumber,
        LayoutConfiguration layoutConfiguration,
        Instant updatedAt,
        Long version
) {
}
