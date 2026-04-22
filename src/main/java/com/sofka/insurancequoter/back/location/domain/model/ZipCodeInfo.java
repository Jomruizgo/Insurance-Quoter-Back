package com.sofka.insurancequoter.back.location.domain.model;

// Domain value object returned by the zip code validation port
public record ZipCodeInfo(
        String zipCode,
        String state,
        String municipality,
        String city,
        String catastrophicZone,
        boolean valid
) {
}
