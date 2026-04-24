package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto;

import java.time.Instant;

// Response body for GET and PUT /v1/quotes/{folio}/general-info
public record GeneralInfoResponse(
        String folioNumber,
        String quoteStatus,
        InsuredDataDto insuredData,
        UnderwritingDataDto underwritingData,
        Instant updatedAt,
        Long version
) {}
