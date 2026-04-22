package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Optional;

public record PatchLocationRequest(
        Optional<String> locationName,
        Optional<String> address,
        Optional<String> zipCode,
        Optional<String> constructionType,
        Optional<Integer> level,
        Optional<Integer> constructionYear,
        Optional<BusinessLineRequest> businessLine,
        Optional<List<GuaranteeRequest>> guarantees,
        @NotNull Long version
) {
    // Default absent fields to Optional.empty() when not present in JSON
    public PatchLocationRequest {
        locationName = locationName != null ? locationName : Optional.empty();
        address = address != null ? address : Optional.empty();
        zipCode = zipCode != null ? zipCode : Optional.empty();
        constructionType = constructionType != null ? constructionType : Optional.empty();
        level = level != null ? level : Optional.empty();
        constructionYear = constructionYear != null ? constructionYear : Optional.empty();
        businessLine = businessLine != null ? businessLine : Optional.empty();
        guarantees = guarantees != null ? guarantees : Optional.empty();
    }
}
