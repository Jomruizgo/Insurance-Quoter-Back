package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuoteMetricsTest {

    @Mock
    private QuoteRepository quoteRepository;

    @Mock
    private CoreServiceClient coreServiceClient;

    private SimpleMeterRegistry meterRegistry;
    private CreateFolioUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new CreateFolioUseCaseImpl(quoteRepository, coreServiceClient, meterRegistry);
    }

    @Test
    void quotesCreatedCounter_increments_withOutcomeNew_whenFolioIsCreated() {
        // GIVEN
        Quote saved = new Quote("FOL-2026-00001", QuoteStatus.CREATED, "SUB-1", "AGT-1", 0L, Instant.now(), Instant.now());
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-1", "AGT-1")).thenReturn(Optional.empty());
        when(coreServiceClient.existsSubscriber("SUB-1")).thenReturn(true);
        when(coreServiceClient.existsAgent("AGT-1")).thenReturn(true);
        when(coreServiceClient.nextFolioNumber()).thenReturn("FOL-2026-00001");
        when(quoteRepository.save(any())).thenReturn(saved);

        // WHEN
        useCase.createFolio(new CreateFolioCommand("SUB-1", "AGT-1"));

        // THEN
        Counter counter = meterRegistry.find("quotes.created").tag("outcome", "new").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void quotesCreatedCounter_increments_withOutcomeExisting_whenFolioAlreadyExists() {
        // GIVEN
        Quote existing = new Quote("FOL-2026-00001", QuoteStatus.CREATED, "SUB-1", "AGT-1", 0L, Instant.now(), Instant.now());
        when(quoteRepository.findActiveBySubscriberAndAgent("SUB-1", "AGT-1")).thenReturn(Optional.of(existing));

        // WHEN
        useCase.createFolio(new CreateFolioCommand("SUB-1", "AGT-1"));

        // THEN
        Counter counter = meterRegistry.find("quotes.created").tag("outcome", "existing").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
