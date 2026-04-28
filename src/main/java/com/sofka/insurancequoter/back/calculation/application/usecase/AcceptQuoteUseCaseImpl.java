package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.domain.model.AcceptQuoteResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.AcceptQuoteUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.out.AcceptQuoteRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;

import java.time.Instant;

public class AcceptQuoteUseCaseImpl implements AcceptQuoteUseCase {

    private final AcceptQuoteRepository repository;
    private final Counter acceptedCounter;

    public AcceptQuoteUseCaseImpl(AcceptQuoteRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.acceptedCounter = Counter.builder("quote.accepted")
                .description("Quotes accepted and issued")
                .register(meterRegistry);
    }

    @Override
    @Observed(name = "quote.accept")
    public AcceptQuoteResult accept(AcceptQuoteCommand command) {
        long newVersion = repository.accept(command);
        acceptedCounter.increment();
        return new AcceptQuoteResult(
                command.folioNumber(),
                "ISSUED",
                command.acceptedBy(),
                Instant.now(),
                newVersion
        );
    }
}
