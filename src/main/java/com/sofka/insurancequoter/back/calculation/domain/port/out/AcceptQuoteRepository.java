package com.sofka.insurancequoter.back.calculation.domain.port.out;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;

// Output port: persists the ISSUED status for an accepted quote
public interface AcceptQuoteRepository {

    long accept(AcceptQuoteCommand command);
}
