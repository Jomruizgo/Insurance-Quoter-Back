package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities.CoverageOptionJpa;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.domain.model.SectionStatus;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter.CoverageOptionsStateJpaAdapter;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageOptionsStateJpaAdapterTest {

    @Mock private QuoteJpaRepository quoteJpaRepository;
    @Mock private CoverageOptionJpaRepository coverageOptionJpaRepository;

    private CoverageOptionsStateJpaAdapter adapter;

    private static final String FOLIO = "FOL-2026-00042";
    private static final Long QUOTE_ID = 1L;

    @BeforeEach
    void setUp() {
        adapter = new CoverageOptionsStateJpaAdapter(quoteJpaRepository, coverageOptionJpaRepository);
    }

    private QuoteJpa buildQuoteJpa() {
        return QuoteJpa.builder()
                .id(QUOTE_ID).folioNumber(FOLIO).quoteStatus("IN_PROGRESS")
                .subscriberId("SUB-001").agentCode("AGT-001").version(1L)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void readByFolioNumber_returnsPending_whenFolioNotFound() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThat(adapter.readByFolioNumber(FOLIO)).isEqualTo(SectionStatus.PENDING);
    }

    @Test
    void readByFolioNumber_returnsPending_whenNoOptionsExist() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(buildQuoteJpa()));
        when(coverageOptionJpaRepository.findAllByQuote_Id(QUOTE_ID)).thenReturn(List.of());

        // WHEN / THEN
        assertThat(adapter.readByFolioNumber(FOLIO)).isEqualTo(SectionStatus.PENDING);
    }

    @Test
    void readByFolioNumber_returnsInProgress_whenOptionsExistButNoneSelected() {
        // GIVEN
        QuoteJpa quote = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quote));
        CoverageOptionJpa unselected = CoverageOptionJpa.builder()
                .id(1L).quote(quote).code("FIRE").selected(false).build();
        when(coverageOptionJpaRepository.findAllByQuote_Id(QUOTE_ID)).thenReturn(List.of(unselected));

        // WHEN / THEN
        assertThat(adapter.readByFolioNumber(FOLIO)).isEqualTo(SectionStatus.IN_PROGRESS);
    }

    @Test
    void readByFolioNumber_returnsComplete_whenAtLeastOneOptionSelected() {
        // GIVEN
        QuoteJpa quote = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quote));
        CoverageOptionJpa selected = CoverageOptionJpa.builder()
                .id(1L).quote(quote).code("FIRE").selected(true).build();
        CoverageOptionJpa unselected = CoverageOptionJpa.builder()
                .id(2L).quote(quote).code("CAT").selected(false).build();
        when(coverageOptionJpaRepository.findAllByQuote_Id(QUOTE_ID)).thenReturn(List.of(selected, unselected));

        // WHEN / THEN
        assertThat(adapter.readByFolioNumber(FOLIO)).isEqualTo(SectionStatus.COMPLETE);
    }
}
