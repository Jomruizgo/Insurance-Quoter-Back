package com.sofka.insurancequoter.back.coverage.application.usecase;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.out.CoverageOptionRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.QuoteLookupPort;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageMetricsTest {

    @Mock private QuoteLookupPort quoteLookupPort;
    @Mock private CoverageOptionRepository coverageOptionRepository;
    @Mock private CoverageDerivationService coverageDerivationService;

    private SimpleMeterRegistry meterRegistry;
    private SaveCoverageOptionsUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        useCase = new SaveCoverageOptionsUseCaseImpl(
                quoteLookupPort, coverageOptionRepository, coverageDerivationService, meterRegistry);
    }

    @Test
    void coverageOptionsSavedCounter_increments_whenOptionsAreSaved() {
        // GIVEN
        CoverageOption option = new CoverageOption("COV-001", "Incendio", true,
                java.math.BigDecimal.valueOf(5), java.math.BigDecimal.valueOf(10));
        SaveCoverageOptionsCommand command = new SaveCoverageOptionsCommand(
                "FOL-001", List.of(option), 1L);

        when(coverageDerivationService.knownDescriptions())
                .thenReturn(Map.of("COV-001", "Incendio"));
        when(coverageOptionRepository.replaceAll(anyString(), any()))
                .thenReturn(List.of(option));
        when(quoteLookupPort.getCurrentVersion("FOL-001")).thenReturn(2L);
        when(quoteLookupPort.getUpdatedAt("FOL-001")).thenReturn(Instant.now());

        // WHEN
        useCase.saveCoverageOptions(command);

        // THEN
        Counter counter = meterRegistry.find("coverage.options.saved").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }
}
