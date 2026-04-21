package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

// Request body for POST /v1/folios
public record CreateFolioRequest(
        @NotBlank String subscriberId,
        @NotBlank String agentCode
) {}
