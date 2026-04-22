package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request.CoverageOptionItemRequest;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request.SaveCoverageOptionsRequest;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response.CoverageOptionItemResponse;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response.CoverageOptionsListResponse;

import java.util.List;

// Maps between REST DTOs and application layer types (commands/responses)
public class CoverageRestMapper {

    public SaveCoverageOptionsCommand toCommand(String folioNumber, SaveCoverageOptionsRequest request) {
        List<CoverageOption> options = request.coverageOptions().stream()
                .map(this::toDomain)
                .toList();
        return new SaveCoverageOptionsCommand(folioNumber, options, request.version());
    }

    private CoverageOption toDomain(CoverageOptionItemRequest req) {
        return new CoverageOption(req.code(), null, req.selected(),
                req.deductiblePercentage(), req.coinsurancePercentage());
    }

    public CoverageOptionsListResponse toListResponse(CoverageOptionsResponse response) {
        List<CoverageOptionItemResponse> items = response.coverageOptions().stream()
                .map(this::toItemResponse)
                .toList();
        return new CoverageOptionsListResponse(
                response.folioNumber(), items, response.updatedAt(), response.version());
    }

    private CoverageOptionItemResponse toItemResponse(CoverageOption option) {
        return new CoverageOptionItemResponse(
                option.code(), option.description(), option.selected(),
                option.deductiblePercentage(), option.coinsurancePercentage());
    }
}
