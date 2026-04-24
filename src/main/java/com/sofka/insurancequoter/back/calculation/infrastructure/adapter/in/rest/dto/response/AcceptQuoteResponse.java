package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response;

import java.time.Instant;

// Response DTO for POST /v1/quotes/{folio}/accept
public record AcceptQuoteResponse(
        String folioNumber,
        String quoteStatus,
        String acceptedBy,
        Instant acceptedAt,
        long version
) {}
