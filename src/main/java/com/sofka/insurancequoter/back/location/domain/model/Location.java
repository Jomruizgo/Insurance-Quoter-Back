package com.sofka.insurancequoter.back.location.domain.model;

import java.util.List;

// Domain model for a single location within a quote, including all tarification data
public record Location(
        int index,
        boolean active,
        String locationName,
        String address,
        String zipCode,
        String state,
        String municipality,
        String neighborhood,
        String city,
        String constructionType,
        Integer level,
        Integer constructionYear,
        BusinessLine businessLine,
        List<Guarantee> guarantees,
        String catastrophicZone,
        ValidationStatus validationStatus,
        List<BlockingAlert> blockingAlerts
) {
}
