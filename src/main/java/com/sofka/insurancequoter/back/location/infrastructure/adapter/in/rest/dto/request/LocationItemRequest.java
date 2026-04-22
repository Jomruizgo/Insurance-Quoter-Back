package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request;

import java.util.List;

public record LocationItemRequest(
        Integer index,
        String locationName,
        String address,
        String zipCode,
        String constructionType,
        Integer level,
        Integer constructionYear,
        BusinessLineRequest businessLine,
        List<GuaranteeRequest> guarantees
) {
}
