package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.mappers.QuotePersistenceMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class QuotePersistenceMapperTest {

    private final QuotePersistenceMapper mapper = new QuotePersistenceMapper();

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T14:30:00Z");

    // --- toJpa ---

    @Test
    void shouldMapAllFields_whenConvertingQuoteToJpa() {
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
        QuoteJpa jpa = mapper.toJpa(quote);

        // THEN
        assertThat(jpa.getFolioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(jpa.getQuoteStatus()).isEqualTo("CREATED");
        assertThat(jpa.getSubscriberId()).isEqualTo("SUB-001");
        assertThat(jpa.getAgentCode()).isEqualTo("AGT-123");
    }

    // --- toDomain ---

    @Test
    void shouldMapAllFields_whenConvertingJpaToDomain() {
        // GIVEN
        QuoteJpa jpa = QuoteJpa.builder()
                .folioNumber("FOL-2026-00042")
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .version(1L)
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();

        // WHEN
        Quote quote = mapper.toDomain(jpa);

        // THEN
        assertThat(quote.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(quote.quoteStatus()).isEqualTo(QuoteStatus.CREATED);
        assertThat(quote.subscriberId()).isEqualTo("SUB-001");
        assertThat(quote.agentCode()).isEqualTo("AGT-123");
        assertThat(quote.version()).isEqualTo(1L);
        assertThat(quote.createdAt()).isEqualTo(FIXED_INSTANT);
        assertThat(quote.updatedAt()).isEqualTo(FIXED_INSTANT);
    }
}
