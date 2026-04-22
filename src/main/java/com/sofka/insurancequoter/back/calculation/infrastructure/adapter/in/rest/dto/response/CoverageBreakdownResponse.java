package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response;

import java.math.BigDecimal;

// Response DTO for the 14-component coverage breakdown per location
public record CoverageBreakdownResponse(
        BigDecimal fireBuildings,
        BigDecimal fireContents,
        BigDecimal coverageExtension,
        BigDecimal cattev,
        BigDecimal catfhm,
        BigDecimal debrisRemoval,
        BigDecimal extraordinaryExpenses,
        BigDecimal rentalLoss,
        BigDecimal businessInterruption,
        BigDecimal electronicEquipment,
        BigDecimal theft,
        BigDecimal cashAndValues,
        BigDecimal glass,
        BigDecimal luminousSignage
) {}
