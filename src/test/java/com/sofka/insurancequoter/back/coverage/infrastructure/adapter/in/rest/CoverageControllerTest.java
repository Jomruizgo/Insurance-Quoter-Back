package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.application.usecase.exception.InvalidCoverageCodeException;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.domain.port.in.GetCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.in.SaveCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.mapper.CoverageRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Controller tests using MockMvc standalone setup — TDD phase
@ExtendWith(MockitoExtension.class)
class CoverageControllerTest {

    @Mock
    private GetCoverageOptionsUseCase getCoverageOptionsUseCase;

    @Mock
    private SaveCoverageOptionsUseCase saveCoverageOptionsUseCase;

    private MockMvc mockMvc;

    private static final String FOLIO = "FOL-2026-00042";
    private static final Instant UPDATED_AT = Instant.parse("2026-04-22T15:45:00Z");

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        CoverageController controller = new CoverageController(
                getCoverageOptionsUseCase, saveCoverageOptionsUseCase, new CoverageRestMapper());

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private CoverageOptionsResponse buildResponse(long version, Instant updatedAt) {
        return new CoverageOptionsResponse(FOLIO, List.of(
                new CoverageOption("GUA-FIRE", "Incendio edificios", true,
                        new BigDecimal("2.0"), new BigDecimal("80.0"))
        ), updatedAt, version);
    }

    // --- #190: GET 200 with options list ---

    @Test
    void shouldReturn200WithOptions_whenGetCoverageOptions() throws Exception {
        // GIVEN
        when(getCoverageOptionsUseCase.getCoverageOptions(FOLIO))
                .thenReturn(buildResponse(6L, null));

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/{folio}/coverage-options", FOLIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("GUA-FIRE"))
                .andExpect(jsonPath("$.coverageOptions[0].description").value("Incendio edificios"))
                .andExpect(jsonPath("$.coverageOptions[0].selected").value(true))
                .andExpect(jsonPath("$.version").value(6));
    }

    // --- #191: GET 404 when folio does not exist ---

    @Test
    void shouldReturn404_whenFolioNotFoundOnGet() throws Exception {
        // GIVEN
        when(getCoverageOptionsUseCase.getCoverageOptions(FOLIO))
                .thenThrow(new FolioNotFoundException(FOLIO));

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/{folio}/coverage-options", FOLIO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    // --- #192: PUT 200 successful save ---

    @Test
    void shouldReturn200WithIncrementedVersion_whenSaveCoverageOptions() throws Exception {
        // GIVEN
        when(saveCoverageOptionsUseCase.saveCoverageOptions(any()))
                .thenReturn(buildResponse(7L, UPDATED_AT));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"GUA-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.version").value(7))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("GUA-FIRE"));
    }

    // --- #193: PUT 409 version conflict ---

    @Test
    void shouldReturn409_whenVersionConflictOnPut() throws Exception {
        // GIVEN
        when(saveCoverageOptionsUseCase.saveCoverageOptions(any()))
                .thenThrow(new VersionConflictException(FOLIO, 6L, 7L));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"GUA-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": 6
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    // --- #194: PUT 422 invalid coverage code ---

    @Test
    void shouldReturn422_whenInvalidCoverageCodeOnPut() throws Exception {
        // GIVEN
        when(saveCoverageOptionsUseCase.saveCoverageOptions(any()))
                .thenThrow(new InvalidCoverageCodeException("COV-INVALID"));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-INVALID","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": 6
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- #195: PUT 422 deductiblePercentage > 100 ---

    @Test
    void shouldReturn422_whenDeductiblePercentageExceedsMaximum() throws Exception {
        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"GUA-FIRE","selected":true,"deductiblePercentage":150.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": 6
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- #196: PUT 422 coinsurancePercentage < 0 ---

    @Test
    void shouldReturn422_whenCoinsurancePercentageBelowMinimum() throws Exception {
        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"GUA-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":-5.0}
                                  ],
                                  "version": 6
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
