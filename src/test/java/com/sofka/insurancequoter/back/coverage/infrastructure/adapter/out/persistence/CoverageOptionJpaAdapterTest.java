package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.adapter.CoverageOptionJpaAdapter;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities.CoverageOptionJpa;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.mappers.CoverageOptionPersistenceMapper;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for CoverageOptionJpaAdapter using Mockito — TDD RED phase
@ExtendWith(MockitoExtension.class)
class CoverageOptionJpaAdapterTest {

    @Mock
    private CoverageOptionJpaRepository coverageOptionJpaRepository;

    @Mock
    private QuoteJpaRepository quoteJpaRepository;

    private CoverageOptionJpaAdapter adapter;

    private static final String FOLIO = "FOL-2026-00042";

    @BeforeEach
    void setUp() {
        adapter = new CoverageOptionJpaAdapter(coverageOptionJpaRepository, quoteJpaRepository,
                new CoverageOptionPersistenceMapper());
    }

    private QuoteJpa buildQuote(long id, long version) {
        return QuoteJpa.builder()
                .id(id)
                .folioNumber(FOLIO)
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .version(version)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // --- #184: findByFolioNumber returns empty list when no options exist ---

    @Test
    void shouldReturnEmptyList_whenNoOptionsExistForFolio() {
        // GIVEN
        QuoteJpa quote = buildQuote(1L, 3L);
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quote));
        when(coverageOptionJpaRepository.findAllByQuote_Id(1L)).thenReturn(List.of());

        // WHEN
        List<CoverageOption> result = adapter.findByFolioNumber(FOLIO);

        // THEN
        assertThat(result).isEmpty();
    }

    // --- #185: replaceAll deletes existing and saves new options ---

    @Test
    void shouldDeleteExistingAndSaveNew_whenReplaceAllIsCalled() {
        // GIVEN
        QuoteJpa quote = buildQuote(1L, 3L);
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quote));
        doNothing().when(coverageOptionJpaRepository).deleteAllByQuote_Id(1L);

        CoverageOption option = new CoverageOption("GUA-FIRE", "Incendio edificios", true,
                new BigDecimal("2.0"), new BigDecimal("80.0"));
        CoverageOptionJpa savedJpa = CoverageOptionJpa.builder()
                .id(10L)
                .quote(quote)
                .code("GUA-FIRE")
                .description("Incendio edificios")
                .selected(true)
                .deductiblePercentage(new BigDecimal("2.0"))
                .coinsurancePercentage(new BigDecimal("80.0"))
                .build();
        when(coverageOptionJpaRepository.saveAll(any())).thenReturn(List.of(savedJpa));
        when(quoteJpaRepository.save(any())).thenReturn(quote);

        // WHEN
        List<CoverageOption> result = adapter.replaceAll(FOLIO, List.of(option));

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("GUA-FIRE");
        assertThat(result.get(0).description()).isEqualTo("Incendio edificios");
        verify(coverageOptionJpaRepository).deleteAllByQuote_Id(1L);
        verify(coverageOptionJpaRepository).saveAll(any());
        verify(quoteJpaRepository).save(any());
    }

    // --- #186: assertFolioExists throws FolioNotFoundException when folio missing ---

    @Test
    void shouldThrowFolioNotFoundException_whenFolioNotFound() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.assertFolioExists(FOLIO))
                .isInstanceOf(FolioNotFoundException.class);
    }

    // --- #187: assertVersionMatches throws VersionConflictException when version differs ---

    @Test
    void shouldThrowVersionConflictException_whenVersionDoesNotMatch() {
        // GIVEN
        QuoteJpa quote = buildQuote(1L, 7L);
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quote));

        // WHEN / THEN
        assertThatThrownBy(() -> adapter.assertVersionMatches(FOLIO, 6L))
                .isInstanceOf(VersionConflictException.class);
    }
}
