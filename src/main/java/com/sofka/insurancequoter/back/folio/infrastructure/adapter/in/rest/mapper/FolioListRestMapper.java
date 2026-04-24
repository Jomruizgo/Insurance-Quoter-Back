package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioListResponseDto;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioSummaryDto;
import org.springframework.stereotype.Component;

import java.util.List;

// Maps domain FolioSummary list to the REST response envelope
@Component
public class FolioListRestMapper {

    public FolioListResponseDto toResponse(List<FolioSummary> summaries) {
        List<FolioSummaryDto> dtos = summaries.stream()
                .map(this::toDto)
                .toList();
        return new FolioListResponseDto(dtos);
    }

    private FolioSummaryDto toDto(FolioSummary s) {
        return new FolioSummaryDto(
                s.folioNumber(),
                s.client(),
                s.agentCode(),
                s.agentName(),
                s.status(),
                s.locationCount(),
                s.completionPct(),
                s.commercialPremium(),
                s.updatedAt()
        );
    }
}
