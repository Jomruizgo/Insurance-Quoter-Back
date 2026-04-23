package com.sofka.insurancequoter.back.calculation.domain.port.out;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;

// Output port: persists a premium calculation result and updates the quote status
public interface CalculationResultRepository {

    // Atomically updates quoteStatus=CALCULATED and upserts the calculation result; returns new quote version
    long persist(String folioNumber, CalculationResult result);
}
