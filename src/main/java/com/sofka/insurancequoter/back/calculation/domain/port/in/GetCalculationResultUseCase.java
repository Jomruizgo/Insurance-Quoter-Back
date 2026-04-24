package com.sofka.insurancequoter.back.calculation.domain.port.in;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;

// Input port: retrieves the stored calculation result for a folio
public interface GetCalculationResultUseCase {

    CalculationResult get(String folioNumber);
}
