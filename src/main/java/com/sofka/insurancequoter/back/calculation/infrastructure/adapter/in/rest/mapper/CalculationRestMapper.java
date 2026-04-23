package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CalculationResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CoverageBreakdownResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.PremiumByLocationResponse;
import org.springframework.stereotype.Component;

import java.util.List;

// Maps CalculationResult domain model to REST response DTOs
@Component
public class CalculationRestMapper {

    public CalculationResponse toResponse(CalculationResult result) {
        List<PremiumByLocationResponse> premiums = result.premiumsByLocation().stream()
                .map(this::toPremiumByLocationResponse)
                .toList();

        return new CalculationResponse(
                result.folioNumber(),
                "CALCULATED",
                result.netPremium(),
                result.commercialPremium(),
                premiums,
                result.calculatedAt(),
                result.version()
        );
    }

    private PremiumByLocationResponse toPremiumByLocationResponse(PremiumByLocation domain) {
        CoverageBreakdownResponse breakdown = domain.coverageBreakdown() != null
                ? toCoverageBreakdownResponse(domain.coverageBreakdown())
                : null;

        return new PremiumByLocationResponse(
                domain.index(),
                domain.locationName(),
                domain.netPremium(),
                domain.commercialPremium(),
                domain.calculable(),
                breakdown,
                domain.blockingAlerts()
        );
    }

    private CoverageBreakdownResponse toCoverageBreakdownResponse(CoverageBreakdown bd) {
        return new CoverageBreakdownResponse(
                bd.fireBuildings(),
                bd.fireContents(),
                bd.coverageExtension(),
                bd.cattev(),
                bd.catfhm(),
                bd.debrisRemoval(),
                bd.extraordinaryExpenses(),
                bd.rentalLoss(),
                bd.businessInterruption(),
                bd.electronicEquipment(),
                bd.theft(),
                bd.cashAndValues(),
                bd.glass(),
                bd.luminousSignage()
        );
    }
}
