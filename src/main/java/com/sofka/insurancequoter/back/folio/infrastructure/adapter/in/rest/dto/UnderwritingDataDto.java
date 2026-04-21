package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

// Nested DTO carrying subscriber and agent identifiers within a folio response
public record UnderwritingDataDto(String subscriberId, String agentCode) {}
