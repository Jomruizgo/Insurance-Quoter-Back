package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.dto;

import java.math.BigDecimal;

// DTO for the tariff data object nested inside TariffResponse from the core service
public record TariffData(
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
