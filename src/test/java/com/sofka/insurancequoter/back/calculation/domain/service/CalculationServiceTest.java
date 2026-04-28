package com.sofka.insurancequoter.back.calculation.domain.service;

import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
import com.sofka.insurancequoter.back.location.domain.model.BusinessLine;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Unit tests for CalculationService — pure domain service, no Spring context
class CalculationServiceTest {

    private CalculationService calculationService;

    // Standard tariff used across most tests
    private static final Tariff TARIFF = new Tariff(
            BigDecimal.valueOf(0.0015),   // fireRate
            BigDecimal.valueOf(0.0012),   // fireContentsRate
            BigDecimal.valueOf(0.07),     // coverageExtensionFactor
            BigDecimal.valueOf(0.0008),   // cattevFactor
            BigDecimal.valueOf(0.0005),   // catfhmFactor
            BigDecimal.valueOf(0.03),     // debrisRemovalFactor
            BigDecimal.valueOf(0.02),     // extraordinaryExpensesFactor
            BigDecimal.valueOf(0.015),    // rentalLossRate
            BigDecimal.valueOf(0.015),    // businessInterruptionRate
            BigDecimal.valueOf(0.002),    // electronicEquipmentRate
            BigDecimal.valueOf(0.003),    // theftRate
            BigDecimal.valueOf(0.005),    // cashAndValuesRate
            BigDecimal.valueOf(0.001),    // glassRate
            BigDecimal.valueOf(0.002),    // luminousSignageRate
            BigDecimal.valueOf(1.16)      // commercialFactor
    );

    private static final Set<String> TARIFABLE_CODES = Set.of("GUA-FIRE", "GUA-CONT", "GUA-THEFT");

    @BeforeEach
    void setUp() {
        calculationService = new CalculationService();
    }

    // --- Helper to build a minimal Location ---

    private Location locationWith(String zipCode, String fireKey, List<Guarantee> guarantees) {
        BusinessLine businessLine = fireKey != null
                ? new BusinessLine("BL-001", fireKey, "Desc")
                : null;
        return new Location(
                1, true, "Test Location", "Av. Test 1",
                zipCode, "Estado", "Municipio", "Col", "Ciudad",
                "MASONRY", 1, 2000,
                businessLine, guarantees,
                "ZONE_A", ValidationStatus.COMPLETE, List.of()
        );
    }

    // ===== isCalculable =====

    @Test
    void isCalculable_returnsTrue_whenZipCodeFireKeyAndTarifableGuaranteePresent() {
        // GIVEN
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        boolean result = calculationService.isCalculable(location, TARIFABLE_CODES);
        // THEN
        assertThat(result).isTrue();
    }

    @Test
    void isCalculable_returnsFalse_whenZipCodeMissing() {
        // GIVEN
        Location location = locationWith(null, "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        boolean result = calculationService.isCalculable(location, TARIFABLE_CODES);
        // THEN
        assertThat(result).isFalse();
    }

    @Test
    void isCalculable_returnsFalse_whenFireKeyMissing() {
        // GIVEN
        Location location = locationWith("06600", null,
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        boolean result = calculationService.isCalculable(location, TARIFABLE_CODES);
        // THEN
        assertThat(result).isFalse();
    }

    @Test
    void isCalculable_returnsFalse_whenNoTarifableGuarantee() {
        // GIVEN — guarantee code not in tarifable set
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-NON-TARIFABLE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        boolean result = calculationService.isCalculable(location, TARIFABLE_CODES);
        // THEN
        assertThat(result).isFalse();
    }

    // ===== getBlockingAlerts =====

    @Test
    void getBlockingAlerts_returnsMissingZipCode_whenZipCodeIsNull() {
        // GIVEN
        Location location = locationWith(null, "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        List<BlockingAlert> alerts = calculationService.getBlockingAlerts(location, TARIFABLE_CODES);
        // THEN
        assertThat(alerts).extracting(BlockingAlert::code)
                .contains(BlockingAlertCode.MISSING_ZIP_CODE.name());
    }

    @Test
    void getBlockingAlerts_returnsMissingFireKey_whenFireKeyIsNull() {
        // GIVEN
        Location location = locationWith("06600", null,
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        List<BlockingAlert> alerts = calculationService.getBlockingAlerts(location, TARIFABLE_CODES);
        // THEN
        assertThat(alerts).extracting(BlockingAlert::code)
                .contains(BlockingAlertCode.MISSING_FIRE_KEY.name());
    }

    @Test
    void getBlockingAlerts_returnsMissingTarifableGuarantee_whenNoTarifableGuarantee() {
        // GIVEN
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-NON-TARIFABLE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        List<BlockingAlert> alerts = calculationService.getBlockingAlerts(location, TARIFABLE_CODES);
        // THEN
        assertThat(alerts).extracting(BlockingAlert::code)
                .contains(BlockingAlertCode.MISSING_TARIFABLE_GUARANTEE.name());
    }

    // ===== calculateLocation — individual components =====

    @Test
    void calculateLocation_fireBuildings_returnsInsuredValueTimesRate() {
        // GIVEN — only GUA-FIRE guarantee: 1,000,000 × 0.0015 = 1,500.00
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.calculable()).isTrue();
        assertThat(result.coverageBreakdown().fireBuildings())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void calculateLocation_fireBuildings_returnsZero_whenNoGUAFIREGuarantee() {
        // GIVEN — only GUA-THEFT (tarifable but not fire buildings)
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-THEFT", BigDecimal.valueOf(500_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().fireBuildings())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void calculateLocation_fireContents_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-CONT: 500,000 × 0.0012 = 600.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-CONT");
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-CONT", BigDecimal.valueOf(500_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().fireContents())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void calculateLocation_fireContents_returnsZero_whenNoGUACONTGuarantee() {
        // GIVEN — only GUA-FIRE
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().fireContents())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void calculateLocation_coverageExtension_returnsFirePremiumTimesFactor() {
        // GIVEN — GUA-FIRE: 1,000,000 × 0.0015 = 1500 (firePremium)
        // coverageExtension = 1500 × 0.07 = 105.00
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().coverageExtension())
                .isEqualByComparingTo(new BigDecimal("105.00"));
    }

    @Test
    void calculateLocation_cattev_returnsFirePremiumTimesFactor() {
        // GIVEN — firePremium = 1500; cattev = 1500 × 0.0008 = 1.20
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().cattev())
                .isEqualByComparingTo(new BigDecimal("1.20"));
    }

    @Test
    void calculateLocation_catfhm_returnsFirePremiumTimesFactor() {
        // GIVEN — firePremium = 1500; catfhm = 1500 × 0.0005 = 0.75
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().catfhm())
                .isEqualByComparingTo(new BigDecimal("0.75"));
    }

    @Test
    void calculateLocation_debrisRemoval_returnsFirePremiumTimesFactor() {
        // GIVEN — firePremium = 1500; debrisRemoval = 1500 × 0.03 = 45.00
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().debrisRemoval())
                .isEqualByComparingTo(new BigDecimal("45.00"));
    }

    @Test
    void calculateLocation_extraordinaryExpenses_returnsFirePremiumTimesFactor() {
        // GIVEN — firePremium = 1500; extraordinaryExpenses = 1500 × 0.02 = 30.00
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().extraordinaryExpenses())
                .isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    void calculateLocation_rentalLoss_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-RENTAL: 200,000 × 0.015 = 3000.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-RENTAL");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-RENTAL", BigDecimal.valueOf(200_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().rentalLoss())
                .isEqualByComparingTo(new BigDecimal("3000.00"));
    }

    @Test
    void calculateLocation_rentalLoss_returnsZero_whenNoGUARENTALGuarantee() {
        // GIVEN — no rental guarantee
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().rentalLoss())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }

    @Test
    void calculateLocation_businessInterruption_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-BI: 300,000 × 0.015 = 4500.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-BI");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-BI", BigDecimal.valueOf(300_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().businessInterruption())
                .isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    void calculateLocation_electronicEquipment_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-ELEC: 100,000 × 0.002 = 200.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-ELEC");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-ELEC", BigDecimal.valueOf(100_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().electronicEquipment())
                .isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void calculateLocation_theft_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-THEFT: 500,000 × 0.003 = 1500.00
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-THEFT", BigDecimal.valueOf(500_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().theft())
                .isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    void calculateLocation_cashAndValues_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-CASH: 50,000 × 0.005 = 250.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-CASH");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-CASH", BigDecimal.valueOf(50_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().cashAndValues())
                .isEqualByComparingTo(new BigDecimal("250.00"));
    }

    @Test
    void calculateLocation_glass_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-GLASS: 80,000 × 0.001 = 80.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-GLASS");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-GLASS", BigDecimal.valueOf(80_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().glass())
                .isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void calculateLocation_luminousSignage_returnsInsuredValueTimesRate() {
        // GIVEN — GUA-SIGN: 40,000 × 0.002 = 80.00
        Set<String> codes = Set.of("GUA-FIRE", "GUA-SIGN");
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-SIGN", BigDecimal.valueOf(40_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, codes);
        // THEN
        assertThat(result.coverageBreakdown().luminousSignage())
                .isEqualByComparingTo(new BigDecimal("80.00"));
    }

    @Test
    void calculateLocation_netPremium_isSumOfAllComponents() {
        // GIVEN — GUA-FIRE: 1,000,000 and GUA-THEFT: 500,000
        // fireBuildings = 1500.00, fireContents = 0, derived from fire = 1500
        // coverageExtension = 105.00, cattev = 1.20, catfhm = 0.75, debrisRemoval = 45.00, extraordinary = 30.00
        // theft = 1500.00
        // netPremium = 1500 + 0 + 105 + 1.20 + 0.75 + 45 + 30 + 0 + 0 + 0 + 1500 + 0 + 0 + 0 = 3181.95
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-THEFT", BigDecimal.valueOf(500_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        BigDecimal expected = new BigDecimal("1500.00")   // fireBuildings
                .add(new BigDecimal("0.00"))              // fireContents
                .add(new BigDecimal("105.00"))            // coverageExtension (1500*0.07)
                .add(new BigDecimal("1.20"))              // cattev (1500*0.0008)
                .add(new BigDecimal("0.75"))              // catfhm (1500*0.0005)
                .add(new BigDecimal("45.00"))             // debrisRemoval (1500*0.03)
                .add(new BigDecimal("30.00"))             // extraordinaryExpenses (1500*0.02)
                .add(new BigDecimal("0.00"))              // rentalLoss
                .add(new BigDecimal("0.00"))              // businessInterruption
                .add(new BigDecimal("0.00"))              // electronicEquipment
                .add(new BigDecimal("1500.00"))           // theft (500000*0.003)
                .add(new BigDecimal("0.00"))              // cashAndValues
                .add(new BigDecimal("0.00"))              // glass
                .add(new BigDecimal("0.00"));             // luminousSignage
        assertThat(result.netPremium()).isEqualByComparingTo(expected.setScale(2, RoundingMode.HALF_UP));
    }

    @Test
    void calculateLocation_commercialPremium_isNetPremiumTimesCommercialFactor() {
        // GIVEN — same as above: netPremium = 3181.95; commercial = 3181.95 × 1.16 = 3691.06
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-THEFT", BigDecimal.valueOf(500_000))
                ));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        BigDecimal expectedNet = result.netPremium();
        BigDecimal expectedCommercial = expectedNet.multiply(BigDecimal.valueOf(1.16))
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(result.commercialPremium()).isEqualByComparingTo(expectedCommercial);
    }

    @Test
    void calculateLocation_returnsPremiumByLocation_withAllComponents() {
        // GIVEN
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.calculable()).isTrue();
        assertThat(result.coverageBreakdown()).isNotNull();
        assertThat(result.blockingAlerts()).isEmpty();
        assertThat(result.index()).isEqualTo(1);
        assertThat(result.locationName()).isEqualTo("Test Location");
    }

    @Test
    void calculateLocation_returnsNonCalculable_whenLocationIncomplete() {
        // GIVEN — no zip code
        Location location = locationWith(null, "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.calculable()).isFalse();
        assertThat(result.netPremium()).isNull();
        assertThat(result.commercialPremium()).isNull();
        assertThat(result.coverageBreakdown()).isNull();
        assertThat(result.blockingAlerts()).isNotEmpty();
    }

    // ===== sumInsuredValueByCode =====

    @Test
    void sumInsuredValueByCode_returnsSum_ofMatchingGuarantees() {
        // GIVEN — two GUA-FIRE guarantees
        Location location = locationWith("06600", "FK-001",
                List.of(
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000)),
                        new Guarantee("GUA-FIRE", BigDecimal.valueOf(500_000))
                ));
        // WHEN — exercise via calculateLocation: fireBuildings = 1,500,000 × 0.0015 = 2250.00
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().fireBuildings())
                .isEqualByComparingTo(new BigDecimal("2250.00"));
    }

    @Test
    void sumInsuredValueByCode_returnsZero_whenNoMatchingGuarantee() {
        // GIVEN — no GUA-RENTAL guarantee
        Location location = locationWith("06600", "FK-001",
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))));
        // WHEN
        PremiumByLocation result = calculationService.calculateLocation(location, TARIFF, TARIFABLE_CODES);
        // THEN
        assertThat(result.coverageBreakdown().rentalLoss())
                .isEqualByComparingTo(BigDecimal.ZERO.setScale(2));
    }
}
