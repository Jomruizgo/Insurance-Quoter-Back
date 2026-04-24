package com.sofka.insurancequoter.back.calculation.application.usecase.exception;

// Thrown when no calculation result exists for the given folio
public class CalculationResultNotFoundException extends RuntimeException {

    public CalculationResultNotFoundException(String folioNumber) {
        super("Calculation result not found for folio: " + folioNumber);
    }
}
