package com.sofka.insurancequoter.back.location.domain.service;

import com.sofka.insurancequoter.back.location.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LocationValidationServiceTest {

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

    @Test
    void nullZipCode_generates_MISSING_ZIP_CODE_alert() {
        Location location = locationWith(null, new BusinessLine("BL-001", "FK-01", "Bodega"), List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.empty());
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_ZIP_CODE.name()));
    }

    @Test
    void emptyZipCode_generates_MISSING_ZIP_CODE_alert() {
        Location location = locationWith("", new BusinessLine("BL-001", "FK-01", "Bodega"), List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.empty());
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_ZIP_CODE.name()));
    }

    @Test
    void zipCodeInfoEmpty_generates_MISSING_ZIP_CODE_alert() {
        Location location = locationWith("99999", new BusinessLine("BL-001", "FK-01", "Bodega"), List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.empty());
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_ZIP_CODE.name()));
    }

    @Test
    void nullBusinessLine_generates_MISSING_FIRE_KEY_alert() {
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        Location location = locationWith("06600", null, List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(zipInfo));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_FIRE_KEY.name()));
    }

    @Test
    void businessLineWithNullFireKey_generates_MISSING_FIRE_KEY_alert() {
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        Location location = locationWith("06600", new BusinessLine("BL-001", null, "Bodega"), List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(zipInfo));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.MISSING_FIRE_KEY.name()));
    }

    @Test
    void emptyGuarantees_generates_NO_TARIFABLE_GUARANTEES_alert() {
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        Location location = locationWith("06600", new BusinessLine("BL-001", "FK-01", "Bodega"), List.of());
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(zipInfo));
        assertThat(alerts).anyMatch(a -> a.code().equals(BlockingAlertCode.NO_TARIFABLE_GUARANTEES.name()));
    }

    @Test
    void validData_generates_no_alerts_and_status_is_COMPLETE() {
        ZipCodeInfo zipInfo = new ZipCodeInfo("06600", "CDMX", "Cuauhtemoc", "CDMX", "ZONE_A", true);
        Location location = locationWith("06600", new BusinessLine("BL-001", "FK-01", "Bodega"), List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1000000))));
        List<BlockingAlert> alerts = service.calculateAlerts(location, Optional.of(zipInfo));
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
