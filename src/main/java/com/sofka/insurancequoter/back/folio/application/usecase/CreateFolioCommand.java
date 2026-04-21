package com.sofka.insurancequoter.back.folio.application.usecase;

// Application-layer command object — carries the intent to create a folio
public record CreateFolioCommand(
        String subscriberId,
        String agentCode
) {}
