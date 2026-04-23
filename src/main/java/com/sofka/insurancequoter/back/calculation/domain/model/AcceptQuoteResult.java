package com.sofka.insurancequoter.back.calculation.domain.model;

import java.time.Instant;

// Domain model representing the outcome of accepting a quote
public record AcceptQuoteResult(
        String folioNumber,
        String quoteStatus,
        String acceptedBy,
        Instant acceptedAt,
        long version
) {}
