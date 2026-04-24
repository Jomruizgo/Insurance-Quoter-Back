package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.domain.model.AcceptQuoteResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.AcceptQuoteUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.out.AcceptQuoteRepository;

import java.time.Instant;

public class AcceptQuoteUseCaseImpl implements AcceptQuoteUseCase {

    private final AcceptQuoteRepository repository;

    public AcceptQuoteUseCaseImpl(AcceptQuoteRepository repository) {
        this.repository = repository;
    }

    @Override
    public AcceptQuoteResult accept(AcceptQuoteCommand command) {
        long newVersion = repository.accept(command);
        return new AcceptQuoteResult(
                command.folioNumber(),
                "ISSUED",
                command.acceptedBy(),
                Instant.now(),
                newVersion
        );
    }
}
