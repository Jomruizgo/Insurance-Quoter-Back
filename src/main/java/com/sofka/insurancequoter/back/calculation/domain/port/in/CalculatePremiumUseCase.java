package com.sofka.insurancequoter.back.calculation.domain.port.in;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;

// Input port: triggers premium calculation for a given quote
public interface CalculatePremiumUseCase {

    CalculationResult calculate(CalculatePremiumCommand command);
}
