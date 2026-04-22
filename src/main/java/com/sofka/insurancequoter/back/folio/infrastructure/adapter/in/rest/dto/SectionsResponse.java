package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

public record SectionsResponse(
        String generalInfo,
        String layout,
        String locations,
        String coverageOptions,
        String calculation
) {}
