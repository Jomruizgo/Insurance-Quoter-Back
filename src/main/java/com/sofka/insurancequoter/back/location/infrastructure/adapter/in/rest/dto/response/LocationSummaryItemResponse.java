package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

public record LocationSummaryItemResponse(
        int index,
        String locationName,
        String validationStatus,
        List<BlockingAlertResponse> blockingAlerts
) {
}
