package com.sofka.insurancequoter.back.calculation.domain.port.out;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;

import java.util.Optional;

// Output port: reads an existing calculation result by folio number
public interface GetCalculationResultRepository {

    Optional<CalculationResult> find(String folioNumber);
}
