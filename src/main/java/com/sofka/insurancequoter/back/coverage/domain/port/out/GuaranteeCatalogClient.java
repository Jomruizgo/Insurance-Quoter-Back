package com.sofka.insurancequoter.back.coverage.domain.port.out;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;

import java.util.List;

// Output port: fetches the guarantee catalog from the core service
public interface GuaranteeCatalogClient {

    List<GuaranteeDto> fetchGuarantees();
}
