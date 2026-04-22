package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response;

import java.util.List;

public record LocationDetailResponse(
        int index,
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
        BusinessLineResponse businessLine,
        List<GuaranteeResponse> guarantees,
        String catastrophicZone,
        String validationStatus,
        List<BlockingAlertResponse> blockingAlerts
) {
}
