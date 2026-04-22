package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter.QuoteLayoutJpaAdapter;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuoteLayoutJpaAdapterTest {

    @Mock
    private QuoteJpaRepository quoteJpaRepository;

    @Mock
    private LocationPersistenceMapper mapper;

    @InjectMocks
    private QuoteLayoutJpaAdapter adapter;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    private QuoteJpa buildQuoteJpa(String folio, Integer numLoc, String locType, Long version) {
        return QuoteJpa.builder()
                .id(1L)
                .folioNumber(folio)
                .numberOfLocations(numLoc)
                .locationType(locType)
                .version(version)
                .updatedAt(NOW)
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .build();
    }

    // findByFolioNumber — found
    @Test
    void shouldReturnQuoteLayoutData_whenFolioExists() {
        // GIVEN
        QuoteJpa jpa = buildQuoteJpa("FOL-2026-00042", 3, "MULTIPLE", 2L);
        QuoteLayoutData expected = new QuoteLayoutData(1L, "FOL-2026-00042", 3, "MULTIPLE", 2L, NOW);
        when(quoteJpaRepository.findByFolioNumber("FOL-2026-00042")).thenReturn(Optional.of(jpa));
        when(mapper.toQuoteLayoutData(jpa)).thenReturn(expected);

        // WHEN
        Optional<QuoteLayoutData> result = adapter.findByFolioNumber("FOL-2026-00042");

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().folioNumber()).isEqualTo("FOL-2026-00042");
    }

    // findByFolioNumber — not found
    @Test
    void shouldReturnEmpty_whenFolioDoesNotExist() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber("FOL-9999-99999")).thenReturn(Optional.empty());

        // WHEN
        Optional<QuoteLayoutData> result = adapter.findByFolioNumber("FOL-9999-99999");

        // THEN
        assertThat(result).isEmpty();
        verify(mapper, never()).toQuoteLayoutData(any());
    }

    // save — updates numberOfLocations and locationType on the existing QuoteJpa
    @Test
    void shouldUpdateQuoteJpaAndReturnMappedData_whenSavingLayout() {
        // GIVEN
        QuoteJpa jpa = buildQuoteJpa("FOL-2026-00042", null, null, 1L);
        QuoteLayoutData inputData = new QuoteLayoutData(1L, "FOL-2026-00042", 3, "MULTIPLE", 1L, NOW);
        when(quoteJpaRepository.findById(1L)).thenReturn(Optional.of(jpa));
        when(quoteJpaRepository.save(any(QuoteJpa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toQuoteLayoutData(any())).thenReturn(inputData);

        // WHEN
        QuoteLayoutData saved = adapter.save(inputData);

        // THEN
        ArgumentCaptor<QuoteJpa> captor = ArgumentCaptor.forClass(QuoteJpa.class);
        verify(quoteJpaRepository).save(captor.capture());
        QuoteJpa persisted = captor.getValue();
        assertThat(persisted.getNumberOfLocations()).isEqualTo(3);
        assertThat(persisted.getLocationType()).isEqualTo("MULTIPLE");
        assertThat(saved).isEqualTo(inputData);
    }
}
