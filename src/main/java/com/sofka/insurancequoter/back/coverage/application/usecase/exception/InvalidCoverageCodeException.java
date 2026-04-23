package com.sofka.insurancequoter.back.coverage.application.usecase.exception;

// Thrown when a coverage code submitted in the request is not present in the guarantee catalog
public class InvalidCoverageCodeException extends RuntimeException {

    private final String invalidCode;

    public InvalidCoverageCodeException(String code) {
        super("Coverage code not found in catalog: " + code);
        this.invalidCode = code;
    }

    public String getInvalidCode() {
        return invalidCode;
    }
}
