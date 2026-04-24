package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

// DTO representing underwriting data in REST requests and responses
public record UnderwritingDataDto(
        @NotBlank String subscriberId,
        @NotBlank String agentCode,
        @NotBlank String riskClassification,
        @NotBlank String businessType
) {}
