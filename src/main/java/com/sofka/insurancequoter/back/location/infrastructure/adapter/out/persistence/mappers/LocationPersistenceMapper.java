package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;
import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.BlockingAlertEmbeddable;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

// Maps between domain/application objects and JPA entities for the location bounded context
@Component
public class LocationPersistenceMapper {

    public QuoteLayoutData toQuoteLayoutData(QuoteJpa jpa) {
        return new QuoteLayoutData(
                jpa.getId(),
                jpa.getFolioNumber(),
                jpa.getNumberOfLocations(),
                jpa.getLocationType(),
                jpa.getVersion(),
                jpa.getUpdatedAt()
        );
    }

    public LocationJpa toLocationJpa(Long quoteId, Location location) {
        List<BlockingAlertEmbeddable> alerts = location.blockingAlerts() == null
                ? new ArrayList<>()
                : location.blockingAlerts().stream()
                        .map(a -> new BlockingAlertEmbeddable(a.code(), a.message()))
                        .toList();

        return LocationJpa.builder()
                .quoteId(quoteId)
                .index(location.index())
                .active(location.active())
                .locationName(location.locationName())
                .address(location.address())
                .zipCode(location.zipCode())
                .state(location.state())
                .municipality(location.municipality())
                .neighborhood(location.neighborhood())
                .city(location.city())
                .constructionType(location.constructionType())
                .level(location.level())
                .constructionYear(location.constructionYear())
                .businessLineCode(location.businessLine() != null ? location.businessLine().code() : null)
                .businessLineFireKey(location.businessLine() != null ? location.businessLine().fireKey() : null)
                .businessLineDescription(location.businessLine() != null ? location.businessLine().description() : null)
                .guarantees(location.guarantees())
                .catastrophicZone(location.catastrophicZone())
                .validationStatus(location.validationStatus() != null
                        ? location.validationStatus().name()
                        : ValidationStatus.INCOMPLETE.name())
                .blockingAlerts(alerts)
                .build();
    }

    public Location toDomain(LocationJpa jpa) {
        BusinessLine businessLine = (jpa.getBusinessLineCode() != null || jpa.getBusinessLineFireKey() != null)
                ? new BusinessLine(jpa.getBusinessLineCode(), jpa.getBusinessLineFireKey(), jpa.getBusinessLineDescription())
                : null;

        List<BlockingAlert> alerts = jpa.getBlockingAlerts() == null
                ? List.of()
                : jpa.getBlockingAlerts().stream()
                        .map(a -> new BlockingAlert(a.getAlertCode(), a.getAlertMessage()))
                        .toList();

        ValidationStatus status = jpa.getValidationStatus() != null
                ? ValidationStatus.valueOf(jpa.getValidationStatus())
                : ValidationStatus.INCOMPLETE;

        return new Location(
                jpa.getIndex(),
                jpa.getActive(),
                jpa.getLocationName(),
                jpa.getAddress(),
                jpa.getZipCode(),
                jpa.getState(),
                jpa.getMunicipality(),
                jpa.getNeighborhood(),
                jpa.getCity(),
                jpa.getConstructionType(),
                jpa.getLevel(),
                jpa.getConstructionYear(),
                businessLine,
                jpa.getGuarantees() != null ? jpa.getGuarantees() : List.of(),
                jpa.getCatastrophicZone(),
                status,
                alerts
        );
    }

    public LocationSummary toSummary(LocationJpa jpa) {
        List<BlockingAlert> alerts = jpa.getBlockingAlerts() == null
                ? List.of()
                : jpa.getBlockingAlerts().stream()
                        .map(a -> new BlockingAlert(a.getAlertCode(), a.getAlertMessage()))
                        .toList();

        ValidationStatus status = jpa.getValidationStatus() != null
                ? ValidationStatus.valueOf(jpa.getValidationStatus())
                : ValidationStatus.INCOMPLETE;

        return new LocationSummary(jpa.getIndex(), jpa.getLocationName(), status, alerts);
    }
}
