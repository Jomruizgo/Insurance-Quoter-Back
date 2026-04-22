package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.dto;

// HTTP response DTO from core service GET /v1/tariffs
public record TariffResponse(TariffData tariffs) {}
