package com.sofka.insurancequoter.back.location.domain.service;

import com.sofka.insurancequoter.back.location.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S100")
class LocationValidationServiceTest {

    private static final String BODEGA = "Bodega";
    private static final String BL_001 = "BL-001";
    private static final String ZIP_CODE = "06600";
    private static final String FIRE_KEY = "FK-01";
    private static final String MUNICIPALITY = "Cuauhtemoc";
    private static final String GUA_FIRE = "GUA-FIRE";
    private static final String ZONE = "ZONE_A";

    private LocationValidationService service;

    @BeforeEach
    void setUp() {
        service = new LocationValidationService();
    }

    private Location locationWith(String zipCode, BusinessLine businessLine, List<Guarantee> guarantees) {
        return new Location(1, true, "Test", null, zipCode, null, null,
                null, null, null, null, null,
                businessLine, guarantees, null, ValidationStatus.INCOMPLETE, List.of());
    }

    private ZipCodeInfo validZipInfo() {
        return new ZipCodeInfo(ZIP_CODE, "CDMX", MUNICIPALITY, "CDMX", ZONE, true);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"99999"})
    void missingOrUnknownZipCode_generates_MISSING_ZIP_CODE_alert(String zipCode) {
        Location location = locationWith(zipCode,
                new BusinessLine(BL_001, FIRE_KEY, BODEGA),
                List.of(new Guarantee(GUA_FIRE, BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.empty());
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_ZIP_CODE.name()));
    }

    @Test
    void nullBusinessLine_generates_MISSING_FIRE_KEY_alert() {
        Location location = locationWith(ZIP_CODE, null,
                List.of(new Guarantee(GUA_FIRE, BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(validZipInfo()));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_FIRE_KEY.name()));
    }

    @Test
    void businessLineWithNullFireKey_generates_MISSING_FIRE_KEY_alert() {
        Location location = locationWith(ZIP_CODE,
                new BusinessLine(BL_001, null, BODEGA),
                List.of(new Guarantee(GUA_FIRE, BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(validZipInfo()));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_FIRE_KEY.name()));
    }

    @Test
    void emptyGuarantees_generates_NO_TARIFABLE_GUARANTEES_alert() {
        Location location = locationWith(ZIP_CODE,
                new BusinessLine(BL_001, FIRE_KEY, BODEGA), List.of());
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(validZipInfo()));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.NO_TARIFABLE_GUARANTEES.name()));
    }

    @Test
    void validData_generates_no_alerts_and_status_is_COMPLETE() {
        Location location = locationWith(ZIP_CODE,
                new BusinessLine(BL_001, FIRE_KEY, BODEGA),
                List.of(new Guarantee(GUA_FIRE, BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(validZipInfo()));
        assertThat(alerts).isEmpty();
        assertThat(service.deriveStatus(alerts)).isEqualTo(ValidationStatus.COMPLETE);
    }

    @Test
    void alertsPresent_status_is_INCOMPLETE() {
        Location location = locationWith(null, null, List.of());
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.empty());
        assertThat(service.deriveStatus(alerts)).isEqualTo(ValidationStatus.INCOMPLETE);
    }
}
