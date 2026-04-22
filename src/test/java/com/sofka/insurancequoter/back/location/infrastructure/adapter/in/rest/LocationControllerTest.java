package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.LocationNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper.LocationRestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class LocationControllerTest {

    @Mock private GetLocationsUseCase getLocationsUseCase;
    @Mock private ReplaceLocationsUseCase replaceLocationsUseCase;
    @Mock private PatchLocationUseCase patchLocationUseCase;
    @Mock private GetLocationsSummaryUseCase getLocationsSummaryUseCase;
    @Mock private LocationRestMapper locationRestMapper;

    private MockMvc mockMvc;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        var controller = new LocationController(
                getLocationsUseCase, replaceLocationsUseCase,
                patchLocationUseCase, getLocationsSummaryUseCase,
                locationRestMapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Location sampleLocation(int index) {
        return new Location(index, true, "Bodega " + index, "Av. Test 100", "06600",
                "CDMX", "Cuauhtemoc", null, "CDMX", "MASONRY", 2, 1990,
                new BusinessLine("BL-001", "FK-01", "Bodega"),
                List.of(new Guarantee("GUA-FIRE", BigDecimal.valueOf(1_000_000))),
                "ZONE_A", ValidationStatus.COMPLETE, List.of());
    }

    @Test
    void GET_locations_returns200_whenFolioExists() throws Exception {
        var result = new GetLocationsUseCase.LocationsResult("FOL-001", List.of(sampleLocation(1)), 4L);
        when(getLocationsUseCase.getLocations("FOL-001")).thenReturn(result);
        when(locationRestMapper.toLocationsListResponse(result))
                .thenReturn(new LocationsListResponse("FOL-001", List.of(), 4L));

        mockMvc.perform(get("/v1/quotes/FOL-001/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-001"))
                .andExpect(jsonPath("$.version").value(4));
    }

    @Test
    void GET_locations_returns404_whenFolioNotFound() throws Exception {
        when(getLocationsUseCase.getLocations("UNKNOWN")).thenThrow(new FolioNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/v1/quotes/UNKNOWN/locations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    @Test
    void PUT_locations_returns200_withVersionIncremented() throws Exception {
        var replaceResult = new ReplaceLocationsUseCase.ReplaceLocationsResult(
                "FOL-001", List.of(sampleLocation(1)), NOW, 5L);
        when(replaceLocationsUseCase.replaceLocations(any())).thenReturn(replaceResult);
        when(locationRestMapper.toLocationsListResponseWithTimestamp(replaceResult))
                .thenReturn(new LocationsListResponseWithTimestamp("FOL-001", List.of(), NOW, 5L));

        String body = """
                {
                  "locations": [{
                    "index": 1,
                    "locationName": "Bodega",
                    "zipCode": "06600",
                    "businessLine": {"code": "BL-001", "fireKey": "FK-01"},
                    "guarantees": [{"code": "GUA-FIRE", "insuredValue": 1000000}]
                  }],
                  "version": 4
                }
                """;

        mockMvc.perform(put("/v1/quotes/FOL-001/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5));
    }

    @Test
    void PUT_locations_returns409_whenVersionConflict() throws Exception {
        when(replaceLocationsUseCase.replaceLocations(any()))
                .thenThrow(new VersionConflictException("FOL-001", 3L, 5L));

        String body = """
                {"locations": [], "version": 3}
                """;

        mockMvc.perform(put("/v1/quotes/FOL-001/locations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void PATCH_location_returns200() throws Exception {
        var patchResult = new PatchLocationUseCase.PatchLocationResult("FOL-001", sampleLocation(1), NOW, 6L);
        when(patchLocationUseCase.patchLocation(any())).thenReturn(patchResult);
        when(locationRestMapper.toPatchWrapperResponse(patchResult))
                .thenReturn(new LocationPatchWrapperResponse("FOL-001", null, NOW, 6L));

        String body = """
                {"locationName": "Bodega Nueva", "version": 5}
                """;

        mockMvc.perform(patch("/v1/quotes/FOL-001/locations/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(6));
    }

    @Test
    void PATCH_location_returns404_whenIndexNotFound() throws Exception {
        when(patchLocationUseCase.patchLocation(any()))
                .thenThrow(new LocationNotFoundException("FOL-001", 99));

        String body = """
                {"locationName": "X", "version": 5}
                """;

        mockMvc.perform(patch("/v1/quotes/FOL-001/locations/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("LOCATION_NOT_FOUND"));
    }

    @Test
    void GET_summary_returns200_withCorrectCounts() throws Exception {
        var summaryResult = new GetLocationsSummaryUseCase.SummaryResult(
                "FOL-001", 2, 1, 1,
                List.of(new LocationSummary(1, "Bodega", ValidationStatus.COMPLETE, List.of())));
        when(getLocationsSummaryUseCase.getSummary("FOL-001")).thenReturn(summaryResult);
        when(locationRestMapper.toSummaryWrapperResponse(summaryResult))
                .thenReturn(new LocationsSummaryWrapperResponse("FOL-001", 2, 1, 1, List.of()));

        mockMvc.perform(get("/v1/quotes/FOL-001/locations/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalLocations").value(2))
                .andExpect(jsonPath("$.completeLocations").value(1))
                .andExpect(jsonPath("$.incompleteLocations").value(1));
    }
}
