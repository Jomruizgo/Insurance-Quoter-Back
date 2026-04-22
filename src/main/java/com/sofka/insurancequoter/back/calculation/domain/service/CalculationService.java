package com.sofka.insurancequoter.back.calculation.domain.service;

import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Pure domain service — no Spring annotations. Calculates per-location premium breakdown.
public class CalculationService {

    private static final int INTERMEDIATE_SCALE = 8;
    private static final int RESULT_SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    // Returns true if the location has all required data to be calculated
    public boolean isCalculable(Location location, Set<String> tarifableCodes) {
        if (isBlank(location.zipCode())) return false;
        if (location.businessLine() == null || isBlank(location.businessLine().fireKey())) return false;
        if (!hasTarifableGuarantee(location, tarifableCodes)) return false;
        return true;
    }

    // Returns the list of blocking alerts explaining why a location is not calculable
    public List<BlockingAlert> getBlockingAlerts(Location location, Set<String> tarifableCodes) {
        List<BlockingAlert> alerts = new ArrayList<>();
        if (isBlank(location.zipCode())) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.MISSING_ZIP_CODE.name(),
                    "Código postal requerido"));
        }
        if (location.businessLine() == null || isBlank(location.businessLine().fireKey())) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.MISSING_FIRE_KEY.name(),
                    "Clave incendio requerida"));
        }
        if (!hasTarifableGuarantee(location, tarifableCodes)) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.MISSING_TARIFABLE_GUARANTEE.name(),
                    "Se requiere al menos una garantía tarifable"));
        }
        return alerts;
    }

    // Calculates the premium for a single location; returns non-calculable result if incomplete
    public PremiumByLocation calculateLocation(Location location, Tariff tariff, Set<String> tarifableCodes) {
        if (!isCalculable(location, tarifableCodes)) {
            List<BlockingAlert> alerts = getBlockingAlerts(location, tarifableCodes);
            return new PremiumByLocation(
                    location.index(), location.locationName(),
                    null, null, false, null, alerts
            );
        }

        BigDecimal fireBuildings = calculateFireBuildings(location, tariff);
        BigDecimal fireContents = calculateFireContents(location, tariff);
        BigDecimal firePremium = fireBuildings.add(fireContents);

        BigDecimal coverageExtension = calculateCoverageExtension(firePremium, tariff);
        BigDecimal cattev = calculateCattev(firePremium, tariff);
        BigDecimal catfhm = calculateCatfhm(firePremium, tariff);
        BigDecimal debrisRemoval = calculateDebrisRemoval(firePremium, tariff);
        BigDecimal extraordinaryExpenses = calculateExtraordinaryExpenses(firePremium, tariff);
        BigDecimal rentalLoss = calculateRentalLoss(location, tariff);
        BigDecimal businessInterruption = calculateBusinessInterruption(location, tariff);
        BigDecimal electronicEquipment = calculateElectronicEquipment(location, tariff);
        BigDecimal theft = calculateTheft(location, tariff);
        BigDecimal cashAndValues = calculateCashAndValues(location, tariff);
        BigDecimal glass = calculateGlass(location, tariff);
        BigDecimal luminousSignage = calculateLuminousSignage(location, tariff);

        BigDecimal netPremium = fireBuildings
                .add(fireContents)
                .add(coverageExtension)
                .add(cattev)
                .add(catfhm)
                .add(debrisRemoval)
                .add(extraordinaryExpenses)
                .add(rentalLoss)
                .add(businessInterruption)
                .add(electronicEquipment)
                .add(theft)
                .add(cashAndValues)
                .add(glass)
                .add(luminousSignage)
                .setScale(RESULT_SCALE, ROUNDING);

        BigDecimal commercialPremium = netPremium
                .multiply(tariff.commercialFactor())
                .setScale(RESULT_SCALE, ROUNDING);

        CoverageBreakdown breakdown = new CoverageBreakdown(
                fireBuildings, fireContents, coverageExtension, cattev, catfhm,
                debrisRemoval, extraordinaryExpenses, rentalLoss, businessInterruption,
                electronicEquipment, theft, cashAndValues, glass, luminousSignage
        );

        return new PremiumByLocation(
                location.index(), location.locationName(),
                netPremium, commercialPremium, true, breakdown, List.of()
        );
    }

    // Sums insured values for all guarantees matching the given code
    private BigDecimal sumInsuredValueByCode(Location location, String code) {
        if (location.guarantees() == null) return BigDecimal.ZERO.setScale(RESULT_SCALE, ROUNDING);
        return location.guarantees().stream()
                .filter(g -> code.equals(g.code()))
                .map(Guarantee::insuredValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateFireBuildings(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-FIRE")
                .multiply(tariff.fireRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateFireContents(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-FIRE-CONT")
                .multiply(tariff.fireContentsRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateCoverageExtension(BigDecimal firePremium, Tariff tariff) {
        return firePremium.multiply(tariff.coverageExtensionFactor())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateCattev(BigDecimal firePremium, Tariff tariff) {
        return firePremium.multiply(tariff.cattevFactor())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateCatfhm(BigDecimal firePremium, Tariff tariff) {
        return firePremium.multiply(tariff.catfhmFactor())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateDebrisRemoval(BigDecimal firePremium, Tariff tariff) {
        return firePremium.multiply(tariff.debrisRemovalFactor())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateExtraordinaryExpenses(BigDecimal firePremium, Tariff tariff) {
        return firePremium.multiply(tariff.extraordinaryExpensesFactor())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateRentalLoss(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-RENTAL")
                .multiply(tariff.rentalLossRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateBusinessInterruption(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-BI")
                .multiply(tariff.businessInterruptionRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateElectronicEquipment(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-ELEC")
                .multiply(tariff.electronicEquipmentRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateTheft(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-THEFT")
                .multiply(tariff.theftRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateCashAndValues(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-CASH")
                .multiply(tariff.cashAndValuesRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateGlass(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-GLASS")
                .multiply(tariff.glassRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private BigDecimal calculateLuminousSignage(Location location, Tariff tariff) {
        return sumInsuredValueByCode(location, "GUA-SIGN")
                .multiply(tariff.luminousSignageRate())
                .setScale(RESULT_SCALE, ROUNDING);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasTarifableGuarantee(Location location, Set<String> tarifableCodes) {
        if (location.guarantees() == null || location.guarantees().isEmpty()) return false;
        return location.guarantees().stream()
                .anyMatch(g -> tarifableCodes.contains(g.code()));
    }
}
