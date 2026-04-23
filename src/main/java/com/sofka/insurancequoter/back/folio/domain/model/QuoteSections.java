package com.sofka.insurancequoter.back.folio.domain.model;

public record QuoteSections(
        SectionStatus generalInfo,
        SectionStatus layout,
        SectionStatus locations,
        SectionStatus coverageOptions,
        SectionStatus calculation
) {}
