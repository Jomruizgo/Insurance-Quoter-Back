package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.NoCalculableLocationsException;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.domain.port.in.CalculatePremiumUseCase;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CalculationResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CoverageBreakdownResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.PremiumByLocationResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.mapper.CalculationRestMapper;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Controller tests using MockMvc standalone setup — no Spring Boot context needed
@ExtendWith(MockitoExtension.class)
class CalculationControllerTest {

    @Mock
    private CalculatePremiumUseCase calculatePremiumUseCase;

    @Mock
    private CalculationRestMapper calculationRestMapper;

    private MockMvc mockMvc;

    private static final String FOLIO = "FOL-2026-00042";
    private static final String URL = "/v1/quotes/" + FOLIO + "/calculate";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        CalculationController controller = new CalculationController(
                calculatePremiumUseCase, calculationRestMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // --- Helpers ---

    private CalculationResult buildFullResult() {
        CoverageBreakdown bd = new CoverageBreakdown(
                new BigDecimal("20000.00"), new BigDecimal("15000.00"),
                new BigDecimal("3500.00"), new BigDecimal("4000.00"),
                new BigDecimal("2500.00"), new BigDecimal("1500.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        PremiumByLocation pbl = new PremiumByLocation(
                1, "Bodega Principal",
                new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                true, bd, List.of());
        return new CalculationResult(FOLIO,
                new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                List.of(pbl), Instant.parse("2026-04-22T16:00:00Z"), 8L);
    }

    private CalculationResponse buildFullResponse() {
        CoverageBreakdownResponse bdResp = new CoverageBreakdownResponse(
                new BigDecimal("20000.00"), new BigDecimal("15000.00"),
                new BigDecimal("3500.00"), new BigDecimal("4000.00"),
                new BigDecimal("2500.00"), new BigDecimal("1500.00"),
                new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("500.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
        PremiumByLocationResponse pblResp = new PremiumByLocationResponse(
                1, "Bodega Principal",
                new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                true, bdResp, List.of());
        return new CalculationResponse(
                FOLIO, "CALCULATED",
                new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                List.of(pblResp), Instant.parse("2026-04-22T16:00:00Z"), 8L);
    }

    // --- Tests ---

    @Test
    void calculate_returns200_withFullBreakdown_onSuccess() throws Exception {
        // GIVEN
        when(calculatePremiumUseCase.calculate(any())).thenReturn(buildFullResult());
        when(calculationRestMapper.toResponse(any())).thenReturn(buildFullResponse());

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.quoteStatus").value("CALCULATED"))
                .andExpect(jsonPath("$.netPremium").value(48500.00))
                .andExpect(jsonPath("$.version").value(8))
                .andExpect(jsonPath("$.premiumsByLocation[0].calculable").value(true))
                .andExpect(jsonPath("$.premiumsByLocation[0].coverageBreakdown.fireBuildings").value(20000.00));
    }

    @Test
    void calculate_returns200_withNullPremiums_forNonCalculableLocation() throws Exception {
        // GIVEN
        PremiumByLocation nonCalculable = new PremiumByLocation(
                2, "Oficina Sur", null, null, false, null,
                List.of(new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "Código postal requerido")));
        CalculationResult result = new CalculationResult(FOLIO,
                new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                List.of(nonCalculable), Instant.now(), 8L);

        PremiumByLocationResponse pblResp = new PremiumByLocationResponse(
                2, "Oficina Sur", null, null, false, null,
                List.of(new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "Código postal requerido")));
        CalculationResponse response = new CalculationResponse(
                FOLIO, "CALCULATED", new BigDecimal("48500.00"), new BigDecimal("56260.00"),
                List.of(pblResp), Instant.now(), 8L);

        when(calculatePremiumUseCase.calculate(any())).thenReturn(result);
        when(calculationRestMapper.toResponse(any())).thenReturn(response);

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumsByLocation[0].calculable").value(false))
                .andExpect(jsonPath("$.premiumsByLocation[0].netPremium").doesNotExist())
                .andExpect(jsonPath("$.premiumsByLocation[0].coverageBreakdown").doesNotExist());
    }

    @Test
    void calculate_returns404_whenFolioNotFound() throws Exception {
        // GIVEN
        when(calculatePremiumUseCase.calculate(any())).thenThrow(new FolioNotFoundException(FOLIO));

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    @Test
    void calculate_returns409_onVersionConflict() throws Exception {
        // GIVEN
        when(calculatePremiumUseCase.calculate(any()))
                .thenThrow(new VersionConflictException(FOLIO, 7L, 8L));

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    @Test
    void calculate_returns422_whenNoCalculableLocations() throws Exception {
        // GIVEN
        when(calculatePremiumUseCase.calculate(any())).thenThrow(new NoCalculableLocationsException());

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_CALCULABLE_LOCATIONS"));
    }

    @Test
    void calculate_returns422_whenVersionIsNull() throws Exception {
        // GIVEN — Bean Validation rejects null version
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": null}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void calculate_returns502_whenCoreServiceUnavailable() throws Exception {
        // GIVEN
        when(calculatePremiumUseCase.calculate(any()))
                .thenThrow(new CoreServiceException("Core service unavailable"));

        // WHEN / THEN
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 7}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CORE_SERVICE_ERROR"));
    }
}
