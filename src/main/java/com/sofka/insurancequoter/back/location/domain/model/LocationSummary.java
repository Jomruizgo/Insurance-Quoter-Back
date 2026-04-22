package com.sofka.insurancequoter.back.location.domain.model;

import java.util.List;

// Reduced projection of a location used in summary responses
public record LocationSummary(
        int index,
        String locationName,
        ValidationStatus validationStatus,
        List<BlockingAlert> blockingAlerts
) {
}
