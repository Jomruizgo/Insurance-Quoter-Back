package com.sofka.insurancequoter.back.location.application.usecase;

// Thrown when a folio number does not match any quote in the database
public class FolioNotFoundException extends RuntimeException {

    public FolioNotFoundException(String folioNumber) {
        super("Folio not found: " + folioNumber);
    }
}
