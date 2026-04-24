package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto;

import java.util.List;

// Response envelope for GET /v1/folios → { "folios": [...] }
public record FolioListResponseDto(List<FolioSummaryDto> folios) {}
