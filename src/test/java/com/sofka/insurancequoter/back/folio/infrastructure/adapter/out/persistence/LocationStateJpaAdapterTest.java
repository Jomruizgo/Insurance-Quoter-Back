package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.folio.domain.model.LocationStateSummary;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter.LocationStateJpaAdapter;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.BlockingAlertEmbeddable;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationStateJpaAdapterTest {

    @Mock
    private QuoteJpaRepository quoteJpaRepository;

    @Mock
    private LocationJpaRepository locationJpaRepository;

    @InjectMocks
    private LocationStateJpaAdapter adapter;

    private QuoteJpa quoteJpa(Long id) {
        QuoteJpa q = new QuoteJpa();
        q.setId(id);
        q.setFolioNumber("FOL-001");
        q.setQuoteStatus("IN_PROGRESS");
        q.setSubscriberId("SUB-001");
        q.setAgentCode("AGT-001");
        q.setVersion(1L);
        return q;
    }

    private LocationJpa location(String validationStatus, List<BlockingAlertEmbeddable> alerts) {
        return LocationJpa.builder()
                .quoteId(10L)
                .index(1)
                .active(true)
                .validationStatus(validationStatus)
                .blockingAlerts(alerts)
                .build();
    }

    @Test
    void readByFolioNumber_withNoLocations_returnsTotalZero() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber("FOL-001")).thenReturn(Optional.of(quoteJpa(10L)));
        when(locationJpaRepository.findByQuoteId(10L)).thenReturn(List.of());

        // WHEN
        LocationStateSummary result = adapter.readByFolioNumber("FOL-001");

        // THEN
        assertThat(result.total()).isEqualTo(0);
        assertThat(result.completeCount()).isEqualTo(0);
        assertThat(result.incompleteCount()).isEqualTo(0);
    }

    @Test
    void readByFolioNumber_withMixedLocations_returnsCorrectCounts() {
        // GIVEN — 3 locations: 2 COMPLETE, 1 INCOMPLETE (has blocking alerts)
        when(quoteJpaRepository.findByFolioNumber("FOL-001")).thenReturn(Optional.of(quoteJpa(10L)));
        when(locationJpaRepository.findByQuoteId(10L)).thenReturn(List.of(
                location("COMPLETE", List.of()),
                location("COMPLETE", List.of()),
                location("INCOMPLETE", List.of(new BlockingAlertEmbeddable("MISSING_ZIP_CODE", "Código postal requerido")))
        ));

        // WHEN
        LocationStateSummary result = adapter.readByFolioNumber("FOL-001");

        // THEN
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.completeCount()).isEqualTo(2);
        assertThat(result.incompleteCount()).isEqualTo(1);
    }

    @Test
    void readByFolioNumber_whenFolioNotFound_returnsEmptySummary() {
        // GIVEN — quote doesn't exist (shouldn't happen in normal flow but guard is needed)
        when(quoteJpaRepository.findByFolioNumber("UNKNOWN")).thenReturn(Optional.empty());

        // WHEN
        LocationStateSummary result = adapter.readByFolioNumber("UNKNOWN");

        // THEN
        assertThat(result.total()).isEqualTo(0);
    }
}
