package com.sofka.insurancequoter.back.calculation.domain.port.out;

import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;

// Output port: fetches tariff rates and factors from the core service
public interface TariffClient {

    // Calls GET /v1/tariffs on the core service; throws CoreServiceException if unavailable
    Tariff fetchTariffs();
}
