package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LocationPersistenceMapperTest {

    private final LocationPersistenceMapper mapper = new LocationPersistenceMapper();

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    // QuoteJpa → QuoteLayoutData
    @Test
    void shouldMapQuoteJpaToQuoteLayoutData() {
        // GIVEN
        QuoteJpa jpa = QuoteJpa.builder()
                .id(5L)
                .folioNumber("FOL-2026-00042")
                .numberOfLocations(3)
                .locationType("MULTIPLE")
                .version(2L)
                .updatedAt(NOW)
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .build();

        // WHEN
        QuoteLayoutData data = mapper.toQuoteLayoutData(jpa);

        // THEN
        assertThat(data.id()).isEqualTo(5L);
        assertThat(data.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(data.numberOfLocations()).isEqualTo(3);
        assertThat(data.locationType()).isEqualTo("MULTIPLE");
        assertThat(data.version()).isEqualTo(2L);
        assertThat(data.updatedAt()).isEqualTo(NOW);
    }

    // Location domain → LocationJpa (new insert)
    @Test
    void shouldMapLocationToJpa_withActiveTrue() {
        // GIVEN
        Location location = new Location(2, true);

        // WHEN
        LocationJpa jpa = mapper.toLocationJpa(42L, location);

        // THEN
        assertThat(jpa.getQuoteId()).isEqualTo(42L);
        assertThat(jpa.getIndex()).isEqualTo(2);
        assertThat(jpa.getActive()).isTrue();
        assertThat(jpa.getId()).isNull(); // new entity, no id yet
    }

    // LocationJpa → Location domain
    @Test
    void shouldMapLocationJpaToDomain() {
        // GIVEN
        LocationJpa jpa = LocationJpa.builder()
                .id(7L)
                .quoteId(42L)
                .index(3)
                .active(false)
                .build();

        // WHEN
        Location location = mapper.toDomain(jpa);

        // THEN
        assertThat(location.index()).isEqualTo(3);
        assertThat(location.active()).isFalse();
    }
}
