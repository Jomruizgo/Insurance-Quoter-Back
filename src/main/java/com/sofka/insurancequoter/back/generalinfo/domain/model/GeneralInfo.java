package com.sofka.insurancequoter.back.generalinfo.domain.model;

import java.time.Instant;

// Domain aggregate projection for general quote information — no JPA or Spring annotations allowed
public record GeneralInfo(
        String folioNumber,
        String quoteStatus,
        InsuredData insuredData,
        UnderwritingInfo underwritingInfo,
        Instant updatedAt,
        Long version
) {}
