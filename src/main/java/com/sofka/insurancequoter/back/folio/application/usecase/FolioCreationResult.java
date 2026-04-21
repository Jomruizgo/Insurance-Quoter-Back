package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;

// Application-layer result — wraps the quote and a flag indicating if it was newly created
public record FolioCreationResult(
        Quote quote,
        boolean created
) {}
