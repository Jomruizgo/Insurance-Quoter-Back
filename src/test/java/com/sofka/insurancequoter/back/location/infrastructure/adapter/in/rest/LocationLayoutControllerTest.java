package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.GetLayoutResult;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutResult;
import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.in.SaveLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper.LocationLayoutRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LocationLayoutControllerTest {

    @Mock
    private GetLocationLayoutUseCase getLayoutUseCase;

    @Mock
    private SaveLocationLayoutUseCase saveLayoutUseCase;

    private MockMvc mockMvc;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new LocationLayoutController(
                        getLayoutUseCase, saveLayoutUseCase, new LocationLayoutRestMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // CRITERIO-1.1: GET 200 with layout configured
    @Test
    void shouldReturn200WithLayout_whenGetLayoutAndFolioExists() throws Exception {
        // GIVEN
        GetLayoutResult result = new GetLayoutResult(
                "FOL-2026-00042",
                new LayoutConfiguration(3, LocationType.MULTIPLE),
                2L
        );
        when(getLayoutUseCase.getLayout("FOL-2026-00042")).thenReturn(result);

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/FOL-2026-00042/locations/layout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00042"))
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(3))
                .andExpect(jsonPath("$.layoutConfiguration.locationType").value("MULTIPLE"))
                .andExpect(jsonPath("$.version").value(2));
    }

    // CRITERIO-1.2: GET 404 when folio not found
    @Test
    void shouldReturn404_whenGetLayoutAndFolioNotFound() throws Exception {
        // GIVEN
        when(getLayoutUseCase.getLayout("FOL-9999-99999"))
                .thenThrow(new FolioNotFoundException("FOL-9999-99999"));

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/FOL-9999-99999/locations/layout"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Folio not found"))
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    // CRITERIO-2.1: PUT 200 saves layout successfully
    @Test
    void shouldReturn200_whenSaveLayoutSucceeds() throws Exception {
        // GIVEN
        SaveLayoutResult result = new SaveLayoutResult(
                "FOL-2026-00042",
                new LayoutConfiguration(3, LocationType.MULTIPLE),
                NOW,
                2L
        );
        when(saveLayoutUseCase.saveLayout(any())).thenReturn(result);

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00042/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfiguration": {
                                    "numberOfLocations": 3,
                                    "locationType": "MULTIPLE"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00042"))
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(3))
                .andExpect(jsonPath("$.version").value(2));
    }

    // CRITERIO-2.5: PUT 404 when folio not found
    @Test
    void shouldReturn404_whenSaveLayoutAndFolioNotFound() throws Exception {
        // GIVEN
        when(saveLayoutUseCase.saveLayout(any()))
                .thenThrow(new FolioNotFoundException("FOL-9999-99999"));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-9999-99999/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfiguration": {
                                    "numberOfLocations": 2,
                                    "locationType": "MULTIPLE"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    // CRITERIO-2.4: PUT 409 on optimistic lock conflict
    @Test
    void shouldReturn409_whenOptimisticLockConflict() throws Exception {
        // GIVEN
        when(saveLayoutUseCase.saveLayout(any()))
                .thenThrow(new OptimisticLockingFailureException("version conflict"));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00042/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfiguration": {
                                    "numberOfLocations": 2,
                                    "locationType": "MULTIPLE"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Optimistic lock conflict"))
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    // CRITERIO-2.6: PUT 422 when layoutConfiguration is null
    @Test
    void shouldReturn422_whenLayoutConfigurationIsMissing() throws Exception {
        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00042/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 1
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // PUT 422 when version is null
    @Test
    void shouldReturn422_whenVersionIsMissing() throws Exception {
        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00042/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfiguration": {
                                    "numberOfLocations": 2,
                                    "locationType": "MULTIPLE"
                                  }
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
