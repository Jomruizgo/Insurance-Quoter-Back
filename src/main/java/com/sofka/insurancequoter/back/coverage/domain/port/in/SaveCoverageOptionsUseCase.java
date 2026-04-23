package com.sofka.insurancequoter.back.coverage.domain.port.in;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;

// Input port: replaces the full list of coverage options for a quote
public interface SaveCoverageOptionsUseCase {

    CoverageOptionsResponse saveCoverageOptions(SaveCoverageOptionsCommand command);
}
