package com.sofka.insurancequoter.back.calculation.domain.port.in;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.domain.model.AcceptQuoteResult;

// Input port: accepts a quote and transitions its status to ISSUED
public interface AcceptQuoteUseCase {

    AcceptQuoteResult accept(AcceptQuoteCommand command);
}
