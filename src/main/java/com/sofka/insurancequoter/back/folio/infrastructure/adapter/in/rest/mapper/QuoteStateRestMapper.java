package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.folio.domain.model.QuoteState;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.QuoteStateResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.SectionsResponse;
import org.springframework.stereotype.Component;

@Component
public class QuoteStateRestMapper {

    public QuoteStateResponse toResponse(QuoteState state) {
        SectionsResponse sections = new SectionsResponse(
                state.sections().generalInfo().name(),
                state.sections().layout().name(),
                state.sections().locations().name(),
                state.sections().coverageOptions().name(),
                state.sections().calculation().name()
        );
        return new QuoteStateResponse(
                state.folioNumber(),
                state.quoteStatus(),
                state.completionPercentage(),
                sections,
                state.version(),
                state.updatedAt()
        );
    }
}
