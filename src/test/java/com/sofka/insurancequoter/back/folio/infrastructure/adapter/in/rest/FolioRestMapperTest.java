package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioRestMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FolioRestMapperTest {

    private final FolioRestMapper mapper = new FolioRestMapper();

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T14:30:00Z");

    @Test
    void shouldMapAllFields_whenConvertingQuoteToFolioResponse() {
        // GIVEN
        Quote quote = new Quote(
                "FOL-2026-00042",
                QuoteStatus.CREATED,
                "SUB-001",
                "AGT-123",
                1L,
                FIXED_INSTANT,
                FIXED_INSTANT
        );

        // WHEN
        FolioResponse response = mapper.toResponse(quote);

        // THEN
        assertThat(response.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(response.quoteStatus()).isEqualTo("CREATED");
        assertThat(response.underwritingData().subscriberId()).isEqualTo("SUB-001");
        assertThat(response.underwritingData().agentCode()).isEqualTo("AGT-123");
        assertThat(response.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(response.version()).isEqualTo(1L);
    }
}
