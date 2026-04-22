package com.sofka.insurancequoter.back.location.application.usecase;

// Thrown when a location index does not exist within a quote
public class LocationNotFoundException extends RuntimeException {

    public LocationNotFoundException(String folioNumber, int index) {
        super("Location index " + index + " not found for folio " + folioNumber);
    }
}
