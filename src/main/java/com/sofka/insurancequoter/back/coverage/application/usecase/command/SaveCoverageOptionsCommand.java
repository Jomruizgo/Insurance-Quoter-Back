package com.sofka.insurancequoter.back.coverage.application.usecase.command;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;

import java.util.List;

// Command carrying the data required to replace coverage options on a quote
public record SaveCoverageOptionsCommand(
        String folioNumber,
        List<CoverageOption> coverageOptions,
        long version
) {}
