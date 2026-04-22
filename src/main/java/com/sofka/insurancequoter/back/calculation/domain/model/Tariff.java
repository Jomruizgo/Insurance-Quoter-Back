package com.sofka.insurancequoter.back.calculation.domain.model;

import java.math.BigDecimal;

// Value object carrying all tariff rates and factors needed for premium calculation
public record Tariff(
        BigDecimal fireRate,
        BigDecimal fireContentsRate,
        BigDecimal coverageExtensionFactor,
        BigDecimal cattevFactor,
        BigDecimal catfhmFactor,
        BigDecimal debrisRemovalFactor,
        BigDecimal extraordinaryExpensesFactor,
        BigDecimal rentalLossRate,
        BigDecimal businessInterruptionRate,
        BigDecimal electronicEquipmentRate,
        BigDecimal theftRate,
        BigDecimal cashAndValuesRate,
        BigDecimal glassRate,
        BigDecimal luminousSignageRate,
        BigDecimal commercialFactor
) {}
