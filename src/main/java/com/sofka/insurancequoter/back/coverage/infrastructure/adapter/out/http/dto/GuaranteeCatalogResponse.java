package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http.dto;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;

import java.util.List;

// HTTP response DTO from core service GET /v1/catalogs/guarantees
public record GuaranteeCatalogResponse(List<GuaranteeDto> guarantees) {}
