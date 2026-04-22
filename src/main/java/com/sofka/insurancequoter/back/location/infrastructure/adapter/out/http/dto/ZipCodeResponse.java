package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.http.dto;

// DTO for the GET /v1/zip-codes/{zipCode} response from the core service
public record ZipCodeResponse(
        String zipCode,
        String state,
        String municipality,
        String city,
        String catastrophicZone,
        Boolean valid
) {
}
