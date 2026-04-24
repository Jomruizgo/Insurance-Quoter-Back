package com.sofka.insurancequoter.back.coverage.domain.service;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TDD: RED phase — tests written before CoverageDerivationService exists
class CoverageDerivationServiceTest {

    private CoverageDerivationService service;

    @BeforeEach
    void setUp() {
        service = new CoverageDerivationService();
    }

    // --- COV-BI is always present with selected=false ---

    @Test
    void shouldIncludeCovBi_always_withSelectedFalse() {
        // GIVEN no active guarantees and no catastrophic zone
        List<String> activeCodes = List.of();

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        assertThat(result).isNotEmpty();
        CoverageOption bi = findByCode(result, "COV-BI");
        assertThat(bi).isNotNull();
        assertThat(bi.selected()).isFalse();
        assertThat(bi.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(bi.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("80.0"));
    }

    // --- GUA-FIRE active → COV-FIRE ---

    @Test
    void shouldIncludeCovFire_whenGuaFireIsActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-FIRE");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        CoverageOption fire = findByCode(result, "COV-FIRE");
        assertThat(fire).isNotNull();
        assertThat(fire.selected()).isTrue();
        assertThat(fire.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("2.0"));
        assertThat(fire.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("80.0"));
    }

    // --- GUA-CONT active → COV-FIRE (same coverage) ---

    @Test
    void shouldIncludeCovFire_whenGuaContIsActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-CONT");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        CoverageOption fire = findByCode(result, "COV-FIRE");
        assertThat(fire).isNotNull();
        assertThat(fire.selected()).isTrue();
    }

    // --- GUA-FIRE and GUA-CONT both active → COV-FIRE appears only once ---

    @Test
    void shouldIncludeCovFireOnlyOnce_whenBothGuaFireAndGuaContAreActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-FIRE", "GUA-CONT");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        long count = result.stream().filter(o -> "COV-FIRE".equals(o.code())).count();
        assertThat(count).isEqualTo(1);
    }

    // --- GUA-THEFT active → COV-THEFT ---

    @Test
    void shouldIncludeCovTheft_whenGuaTheftIsActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-THEFT");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        CoverageOption theft = findByCode(result, "COV-THEFT");
        assertThat(theft).isNotNull();
        assertThat(theft.selected()).isTrue();
        assertThat(theft.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(theft.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    // --- GUA-GLASS active → COV-GLASS ---

    @Test
    void shouldIncludeCovGlass_whenGuaGlassIsActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-GLASS");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        CoverageOption glass = findByCode(result, "COV-GLASS");
        assertThat(glass).isNotNull();
        assertThat(glass.selected()).isTrue();
        assertThat(glass.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("5.0"));
        assertThat(glass.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    // --- GUA-ELEC active → COV-ELEC ---

    @Test
    void shouldIncludeCovElec_whenGuaElecIsActive() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-ELEC");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        CoverageOption elec = findByCode(result, "COV-ELEC");
        assertThat(elec).isNotNull();
        assertThat(elec.selected()).isTrue();
        assertThat(elec.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("10.0"));
        assertThat(elec.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("100.0"));
    }

    // --- hasCatZone=true → COV-CAT included ---

    @Test
    void shouldIncludeCovCat_whenHasCatastrophicZone() {
        // GIVEN no active guarantees, but has catastrophic zone
        List<String> activeCodes = List.of();

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, true);

        // THEN
        CoverageOption cat = findByCode(result, "COV-CAT");
        assertThat(cat).isNotNull();
        assertThat(cat.selected()).isTrue();
        assertThat(cat.deductiblePercentage()).isEqualByComparingTo(new BigDecimal("3.0"));
        assertThat(cat.coinsurancePercentage()).isEqualByComparingTo(new BigDecimal("90.0"));
    }

    // --- hasCatZone=false → COV-CAT NOT included ---

    @Test
    void shouldNotIncludeCovCat_whenNoCatastrophicZone() {
        // GIVEN
        List<String> activeCodes = List.of();

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        assertThat(findByCode(result, "COV-CAT")).isNull();
    }

    // --- No active codes, no cat zone → only COV-BI ---

    @Test
    void shouldReturnOnlyCovBi_whenNoActiveGuaranteesAndNoCatZone() {
        // GIVEN
        List<String> activeCodes = List.of();

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("COV-BI");
    }

    // --- Full scenario: all guarantees + cat zone → all 6 coverages ---

    @Test
    void shouldReturnAllCoverages_whenAllGuaranteesActiveAndCatZone() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-FIRE", "GUA-CONT", "GUA-THEFT", "GUA-GLASS", "GUA-ELEC");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, true);

        // THEN
        assertThat(result).hasSize(6); // COV-FIRE, COV-THEFT, COV-GLASS, COV-ELEC, COV-CAT, COV-BI
        assertThat(result.stream().map(CoverageOption::code))
                .containsExactlyInAnyOrder("COV-FIRE", "COV-THEFT", "COV-GLASS", "COV-ELEC", "COV-CAT", "COV-BI");
    }

    // --- Unknown guarantee codes are ignored ---

    @Test
    void shouldIgnoreUnknownGuaranteeCodes() {
        // GIVEN
        List<String> activeCodes = List.of("GUA-UNKNOWN", "GUA-WHATEVER");

        // WHEN
        List<CoverageOption> result = service.deriveFrom(activeCodes, false);

        // THEN — only COV-BI is present
        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("COV-BI");
    }

    // Helper
    private CoverageOption findByCode(List<CoverageOption> options, String code) {
        return options.stream()
                .filter(o -> code.equals(o.code()))
                .findFirst()
                .orElse(null);
    }
}
