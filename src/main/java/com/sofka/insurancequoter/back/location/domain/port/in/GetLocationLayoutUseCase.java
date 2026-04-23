package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.application.usecase.GetLayoutResult;

// Input port: retrieves the layout configuration for a given folio
public interface GetLocationLayoutUseCase {

    GetLayoutResult getLayout(String folioNumber);
}
