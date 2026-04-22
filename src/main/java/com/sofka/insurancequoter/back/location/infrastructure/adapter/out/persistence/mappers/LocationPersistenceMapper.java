package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import org.springframework.stereotype.Component;

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
        return LocationJpa.builder()
                .quoteId(quoteId)
                .index(location.index())
                .active(location.active())
                .build();
    }

    public Location toDomain(LocationJpa jpa) {
        return new Location(jpa.getIndex(), jpa.getActive());
    }
}
