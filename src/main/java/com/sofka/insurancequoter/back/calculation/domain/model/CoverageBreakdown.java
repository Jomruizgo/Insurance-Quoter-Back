package com.sofka.insurancequoter.back.calculation.domain.model;

import java.math.BigDecimal;

// Value object representing the 14-component premium breakdown for a single location
public record CoverageBreakdown(
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
