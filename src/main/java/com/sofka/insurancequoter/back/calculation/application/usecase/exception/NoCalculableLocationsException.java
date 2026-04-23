package com.sofka.insurancequoter.back.calculation.application.usecase.exception;

// Thrown when all locations in a quote are non-calculable (missing required data)
public class NoCalculableLocationsException extends RuntimeException {

    public NoCalculableLocationsException() {
        super("No calculable locations");
    }
}
