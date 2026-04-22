package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.domain.model.*;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.QuoteStateResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.QuoteStateRestMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class QuoteStateRestMapperTest {

    private final QuoteStateRestMapper mapper = new QuoteStateRestMapper();

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    @Test
    void toResponse_mapsAllFieldsCorrectly() {
        // GIVEN
        QuoteSections sections = new QuoteSections(
                SectionStatus.PENDING,
                SectionStatus.COMPLETE,
                SectionStatus.INCOMPLETE,
                SectionStatus.PENDING,
                SectionStatus.PENDING
        );
        QuoteState state = new QuoteState("FOL-001", "IN_PROGRESS", 20, sections, 5L, NOW);

        // WHEN
        QuoteStateResponse response = mapper.toResponse(state);

        // THEN
        assertThat(response.folioNumber()).isEqualTo("FOL-001");
        assertThat(response.quoteStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.completionPercentage()).isEqualTo(20);
        assertThat(response.version()).isEqualTo(5L);
        assertThat(response.updatedAt()).isEqualTo(NOW);
        assertThat(response.sections().generalInfo()).isEqualTo("PENDING");
        assertThat(response.sections().layout()).isEqualTo("COMPLETE");
        assertThat(response.sections().locations()).isEqualTo("INCOMPLETE");
        assertThat(response.sections().coverageOptions()).isEqualTo("PENDING");
        assertThat(response.sections().calculation()).isEqualTo("PENDING");
    }
}
