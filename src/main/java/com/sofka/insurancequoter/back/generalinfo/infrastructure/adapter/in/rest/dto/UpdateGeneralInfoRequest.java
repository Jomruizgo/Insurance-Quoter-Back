package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

// Request body for PUT /v1/quotes/{folio}/general-info
public record UpdateGeneralInfoRequest(
        @NotNull @Valid InsuredDataDto insuredData,
        @NotNull @Valid UnderwritingDataDto underwritingData,
        @NotNull Long version
) {}
