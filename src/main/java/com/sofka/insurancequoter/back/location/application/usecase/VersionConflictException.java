package com.sofka.insurancequoter.back.location.application.usecase;

// Thrown when the version supplied by the client does not match the stored version (optimistic lock)
public class VersionConflictException extends RuntimeException {

    public VersionConflictException(String folioNumber, Long expected, Long actual) {
        super("Version conflict on folio " + folioNumber
                + ": expected " + expected + " but got " + actual);
    }
}
