package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.location.application.usecase.GetLayoutResult;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutCommand;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutResult;
import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.GetLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.LayoutConfigurationDto;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper.LocationLayoutRestMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class LocationLayoutRestMapperTest {

    private final LocationLayoutRestMapper mapper = new LocationLayoutRestMapper();

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    // GetLayoutResult → GetLayoutResponse
    @Test
    void shouldMapGetLayoutResultToResponse() {
        // GIVEN
        GetLayoutResult result = new GetLayoutResult(
                "FOL-2026-00042",
                new LayoutConfiguration(3, LocationType.MULTIPLE),
                2L
        );

        // WHEN
        GetLayoutResponse response = mapper.toGetResponse(result);

        // THEN
        assertThat(response.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(response.version()).isEqualTo(2L);
        assertThat(response.layoutConfiguration().numberOfLocations()).isEqualTo(3);
        assertThat(response.layoutConfiguration().locationType()).isEqualTo("MULTIPLE");
    }

    // GetLayoutResult with null layout → response has null fields
    @Test
    void shouldMapNullLayoutConfigurationToNullFields() {
        // GIVEN
        GetLayoutResult result = new GetLayoutResult(
                "FOL-2026-00042",
                new LayoutConfiguration(null, null),
                1L
        );

        // WHEN
        GetLayoutResponse response = mapper.toGetResponse(result);

        // THEN
        assertThat(response.layoutConfiguration().numberOfLocations()).isNull();
        assertThat(response.layoutConfiguration().locationType()).isNull();
    }

    // SaveLayoutRequest + folio → SaveLayoutCommand
    @Test
    void shouldMapSaveRequestToCommand() {
        // GIVEN
        SaveLayoutRequest request = new SaveLayoutRequest(
                new LayoutConfigurationDto(2, "SINGLE"),
                3L
        );

        // WHEN
        SaveLayoutCommand command = mapper.toCommand("FOL-2026-00042", request);

        // THEN
        assertThat(command.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(command.version()).isEqualTo(3L);
        assertThat(command.layoutConfiguration().numberOfLocations()).isEqualTo(2);
        assertThat(command.layoutConfiguration().locationType()).isEqualTo(LocationType.SINGLE);
    }

    // SaveLayoutResult → SaveLayoutResponse
    @Test
    void shouldMapSaveLayoutResultToResponse() {
        // GIVEN
        SaveLayoutResult result = new SaveLayoutResult(
                "FOL-2026-00042",
                new LayoutConfiguration(2, LocationType.SINGLE),
                NOW,
                4L
        );

        // WHEN
        SaveLayoutResponse response = mapper.toSaveResponse(result);

        // THEN
        assertThat(response.folioNumber()).isEqualTo("FOL-2026-00042");
        assertThat(response.updatedAt()).isEqualTo(NOW);
        assertThat(response.version()).isEqualTo(4L);
        assertThat(response.layoutConfiguration().locationType()).isEqualTo("SINGLE");
    }
}
