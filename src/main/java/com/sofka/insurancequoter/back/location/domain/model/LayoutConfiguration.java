package com.sofka.insurancequoter.back.location.domain.model;

// Value object representing how many locations a quote has and their type
public record LayoutConfiguration(Integer numberOfLocations, LocationType locationType) {
}
