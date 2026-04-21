package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;

// Result returned by GetLocationLayoutUseCase
public record GetLayoutResult(
        String folioNumber,
        LayoutConfiguration layoutConfiguration,
        Long version
) {
}
