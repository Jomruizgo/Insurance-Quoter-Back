package com.sofka.insurancequoter.back.folio.application.usecase;

// Thrown when a subscriber or agent reference does not exist in the core service
public class InvalidReferenceException extends RuntimeException {

    public InvalidReferenceException(String message) {
        super(message);
    }
}
