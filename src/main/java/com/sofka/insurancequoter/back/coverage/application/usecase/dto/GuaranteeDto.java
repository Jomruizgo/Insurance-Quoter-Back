package com.sofka.insurancequoter.back.coverage.application.usecase.dto;

// DTO for guarantee catalog entries fetched from the core service
public record GuaranteeDto(String code, String description, boolean tarifable) {}
