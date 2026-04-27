package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.domain.port.out.AcceptQuoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcceptQuoteMetricsTest {

    @Mock
    private AcceptQuoteRepository repository;

    private SimpleMeterRegistry meterRegistry;
    private AcceptQuoteUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new AcceptQuoteUseCaseImpl(repository, meterRegistry);
    }

    @Test
    void quoteAcceptedCounter_increments_whenQuoteIsAccepted() {
        // GIVEN
        when(repository.accept(any())).thenReturn(2L);

        // WHEN
        useCase.accept(new AcceptQuoteCommand("FOL-001", "USER-1", 1L));

        // THEN
        Counter counter = meterRegistry.find("quote.accepted").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
