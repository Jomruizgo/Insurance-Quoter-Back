package com.sofka.insurancequoter.back.generalinfo.domain.model;

// Value object containing underwriting-related data for a quote
public record UnderwritingInfo(
        String subscriberId,
        String agentCode,
        RiskClassification riskClassification,
        BusinessType businessType
) {}
