package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;

// Command sent to SaveLocationLayoutUseCase
public record SaveLayoutCommand(
        String folioNumber,
        LayoutConfiguration layoutConfiguration,
        Long version
) {
}
