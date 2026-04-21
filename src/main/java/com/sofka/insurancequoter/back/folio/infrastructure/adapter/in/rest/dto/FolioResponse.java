package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

// Response body for POST /v1/folios — matches the api-contracts.md contract exactly
public record FolioResponse(
        String folioNumber,
        String quoteStatus,
        UnderwritingDataDto underwritingData,
        Instant createdAt,
        Long version
) {}
