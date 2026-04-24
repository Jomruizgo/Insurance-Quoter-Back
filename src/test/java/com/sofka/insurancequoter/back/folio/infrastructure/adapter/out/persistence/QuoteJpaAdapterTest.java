package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter.QuoteJpaAdapter;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.mappers.QuotePersistenceMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteJpaAdapterTest {

    @Mock
    private QuoteJpaRepository jpaRepository;

    @Spy
    private QuotePersistenceMapper mapper = new QuotePersistenceMapper();

    @InjectMocks
    private QuoteJpaAdapter adapter;

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-20T14:30:00Z");

    // --- findActiveBySubscriberAndAgent ---

    @Test
    void shouldReturnEmpty_whenNoCreatedFolioExistsForSubscriberAndAgent() {
        // GIVEN
        when(jpaRepository.findBySubscriberIdAndAgentCodeAndQuoteStatus(
                "SUB-001", "AGT-123", "CREATED"))
                .thenReturn(Optional.empty());

        // WHEN
        Optional<Quote> result = adapter.findActiveBySubscriberAndAgent("SUB-001", "AGT-123");

        // THEN
        assertThat(result).isEmpty();
        verify(jpaRepository).findBySubscriberIdAndAgentCodeAndQuoteStatus(
                "SUB-001", "AGT-123", "CREATED");
    }

    // --- save ---

    @Test
    void shouldPersistQuoteAndReturnDomainWithVersion_whenSaving() {
        // GIVEN
        Quote quoteToSave = new Quote(
                "FOL-2026-00042",
                QuoteStatus.CREATED,
                "SUB-001",
                "AGT-123",
                null,
                FIXED_INSTANT,
                FIXED_INSTANT
        );
        QuoteJpa savedJpa = QuoteJpa.builder()
                .id(1L)
                .folioNumber("FOL-2026-00042")
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .version(1L)
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();
        when(jpaRepository.save(any(QuoteJpa.class))).thenReturn(savedJpa);

        // WHEN
        Quote saved = adapter.save(quoteToSave);

        // THEN
        assertThat(saved.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(saved.quoteStatus()).isEqualTo(QuoteStatus.CREATED);
        assertThat(saved.version()).isEqualTo(1L);
        verify(jpaRepository).save(any(QuoteJpa.class));
    }
}
