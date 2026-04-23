package com.sofka.insurancequoter.back.location.domain.model;

// Value object representing a blocking alert that prevents tarification of a location
public record BlockingAlert(String code, String message) {
}
