package com.sofka.insurancequoter.back.coverage.domain.port.in;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;

// Input port: retrieves coverage options persisted for a given quote folio
public interface GetCoverageOptionsUseCase {

    CoverageOptionsResponse getCoverageOptions(String folioNumber);
}
