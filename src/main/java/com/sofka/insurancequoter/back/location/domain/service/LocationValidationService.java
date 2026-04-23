package com.sofka.insurancequoter.back.location.domain.service;

import com.sofka.insurancequoter.back.location.domain.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Domain service that calculates blocking alerts and validation status for a location
public class LocationValidationService {

    public List<BlockingAlert> calculateAlerts(Location location, Optional<ZipCodeInfo> zipCodeInfo) {
        List<BlockingAlert> alerts = new ArrayList<>();

        // MISSING_ZIP_CODE: zip code is blank or the core service did not recognise it
        if (isBlank(location.zipCode()) || zipCodeInfo.isEmpty()) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.MISSING_ZIP_CODE.name(),
                    "Código postal requerido"));
        }

        // MISSING_FIRE_KEY: business line absent or fire key not set
        if (location.businessLine() == null || isBlank(location.businessLine().fireKey())) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.MISSING_FIRE_KEY.name(),
                    "Clave incendio requerida"));
        }

        // NO_TARIFABLE_GUARANTEES: no guarantees provided at all
        if (location.guarantees() == null || location.guarantees().isEmpty()) {
            alerts.add(new BlockingAlert(
                    BlockingAlertCode.NO_TARIFABLE_GUARANTEES.name(),
                    "Se requiere al menos una garantía tarifable"));
        }

        return alerts;
    }

    public ValidationStatus deriveStatus(List<BlockingAlert> alerts) {
        return alerts.isEmpty() ? ValidationStatus.COMPLETE : ValidationStatus.INCOMPLETE;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
