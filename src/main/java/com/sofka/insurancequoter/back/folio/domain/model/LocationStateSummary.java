package com.sofka.insurancequoter.back.folio.domain.model;

// Aggregated location validation counts for state section evaluation
public record LocationStateSummary(
        int total,
        long completeCount,
        long incompleteCount
) {}
