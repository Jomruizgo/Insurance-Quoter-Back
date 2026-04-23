package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.mappers;

import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.CalculationResultJpa;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.PremiumBlockingAlertEmbeddable;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.PremiumByLocationJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Maps between CalculationResult domain model and JPA entities
@Component
public class CalculationPersistenceMapper {

    public CalculationResultJpa toJpa(CalculationResult result, QuoteJpa quote) {
        return CalculationResultJpa.builder()
                .quote(quote)
                .netPremium(result.netPremium())
                .commercialPremium(result.commercialPremium())
                .calculatedAt(result.calculatedAt())
                .premiumsByLocation(new ArrayList<>())
                .build();
    }

    public CalculationResult toDomain(CalculationResultJpa jpa) {
        List<PremiumByLocation> premiums = jpa.getPremiumsByLocation() == null
                ? List.of()
                : jpa.getPremiumsByLocation().stream()
                        .map(this::toPremiumByLocationDomain)
                        .toList();

        return new CalculationResult(
                jpa.getQuote().getFolioNumber(),
                jpa.getNetPremium(),
                jpa.getCommercialPremium(),
                premiums,
                jpa.getCalculatedAt(),
                jpa.getQuote().getVersion()
        );
    }

    public PremiumByLocationJpa toPremiumByLocationJpa(PremiumByLocation domain, CalculationResultJpa jpa) {
        List<PremiumBlockingAlertEmbeddable> alerts = domain.blockingAlerts() == null
                ? new ArrayList<>()
                : domain.blockingAlerts().stream()
                        .map(a -> new PremiumBlockingAlertEmbeddable(a.code(), a.message()))
                        .toList();

        PremiumByLocationJpa.PremiumByLocationJpaBuilder builder = PremiumByLocationJpa.builder()
                .calculationResult(jpa)
                .locationIndex(domain.index())
                .locationName(domain.locationName())
                .netPremium(domain.netPremium())
                .commercialPremium(domain.commercialPremium())
                .calculable(domain.calculable())
                .blockingAlerts(new ArrayList<>(alerts));

        if (domain.coverageBreakdown() != null) {
            CoverageBreakdown bd = domain.coverageBreakdown();
            builder.fireBuildings(bd.fireBuildings())
                    .fireContents(bd.fireContents())
                    .coverageExtension(bd.coverageExtension())
                    .cattev(bd.cattev())
                    .catfhm(bd.catfhm())
                    .debrisRemoval(bd.debrisRemoval())
                    .extraordinaryExpenses(bd.extraordinaryExpenses())
                    .rentalLoss(bd.rentalLoss())
                    .businessInterruption(bd.businessInterruption())
                    .electronicEquipment(bd.electronicEquipment())
                    .theft(bd.theft())
                    .cashAndValues(bd.cashAndValues())
                    .glass(bd.glass())
                    .luminousSignage(bd.luminousSignage());
        }

        return builder.build();
    }

    public PremiumByLocation toPremiumByLocationDomain(PremiumByLocationJpa jpa) {
        List<BlockingAlert> alerts = jpa.getBlockingAlerts() == null
                ? List.of()
                : jpa.getBlockingAlerts().stream()
                        .map(a -> new BlockingAlert(a.getAlertCode(), a.getAlertMessage()))
                        .toList();

        CoverageBreakdown breakdown = jpa.isCalculable() ? new CoverageBreakdown(
                jpa.getFireBuildings(),
                jpa.getFireContents(),
                jpa.getCoverageExtension(),
                jpa.getCattev(),
                jpa.getCatfhm(),
                jpa.getDebrisRemoval(),
                jpa.getExtraordinaryExpenses(),
                jpa.getRentalLoss(),
                jpa.getBusinessInterruption(),
                jpa.getElectronicEquipment(),
                jpa.getTheft(),
                jpa.getCashAndValues(),
                jpa.getGlass(),
                jpa.getLuminousSignage()
        ) : null;

        return new PremiumByLocation(
                jpa.getLocationIndex(),
                jpa.getLocationName(),
                jpa.getNetPremium(),
                jpa.getCommercialPremium(),
                jpa.isCalculable(),
                breakdown,
                alerts
        );
    }
}
