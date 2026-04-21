package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.UnderwritingDataDto;

// Maps domain Quote to the REST FolioResponse DTO
public class FolioRestMapper {

    public FolioResponse toResponse(Quote quote) {
        return new FolioResponse(
                quote.folioNumber(),
                quote.quoteStatus().name(),
                new UnderwritingDataDto(quote.subscriberId(), quote.agentCode()),
                quote.createdAt(),
                quote.version()
        );
    }
}
