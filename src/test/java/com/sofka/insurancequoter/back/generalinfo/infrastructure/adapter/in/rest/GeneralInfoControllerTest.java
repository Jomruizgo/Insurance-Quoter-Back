package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.GlobalExceptionHandler;
import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.GetGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.UpdateGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.mapper.GeneralInfoRestMapper;
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

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GeneralInfoControllerTest {

    @Mock
    private GetGeneralInfoUseCase getGeneralInfoUseCase;

    @Mock
    private UpdateGeneralInfoUseCase updateGeneralInfoUseCase;

    private MockMvc mockMvc;

    private static final Instant FIXED = Instant.parse("2026-04-23T19:55:12Z");

    private GeneralInfo buildGeneralInfo() {
        return new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                FIXED,
                1L
        );
    }

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new GeneralInfoController(
                        getGeneralInfoUseCase,
                        updateGeneralInfoUseCase,
                        new GeneralInfoRestMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    // --- GET happy path ---

    @Test
    void getGeneralInfo_whenFolioExists_returns200WithBody() throws Exception {
        // GIVEN
        when(getGeneralInfoUseCase.getGeneralInfo("FOL-2026-00001"))
                .thenReturn(buildGeneralInfo());

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/FOL-2026-00001/general-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00001"))
                .andExpect(jsonPath("$.quoteStatus").value("CREATED"))
                .andExpect(jsonPath("$.insuredData.name").value("Empresa SA"))
                .andExpect(jsonPath("$.insuredData.rfc").value("EMP123456ABC"))
                .andExpect(jsonPath("$.underwritingData.subscriberId").value("SUB-001"))
                .andExpect(jsonPath("$.underwritingData.riskClassification").value("STANDARD"))
                .andExpect(jsonPath("$.version").value(1));
    }

    // --- GET 404 ---

    @Test
    void getGeneralInfo_whenFolioNotFound_returns404() throws Exception {
        // GIVEN
        when(getGeneralInfoUseCase.getGeneralInfo("UNKNOWN"))
                .thenThrow(new FolioNotFoundException("UNKNOWN"));

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/UNKNOWN/general-info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    // --- PUT happy path ---

    @Test
    void updateGeneralInfo_whenValid_returns200WithUpdatedBody() throws Exception {
        // GIVEN
        GeneralInfo updated = new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                FIXED,
                2L
        );
        when(updateGeneralInfoUseCase.updateGeneralInfo(any())).thenReturn(updated);

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00001/general-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "insuredData": {
                                    "name": "Empresa SA",
                                    "rfc": "EMP123456ABC",
                                    "email": "empresa@test.com",
                                    "phone": "5551234567"
                                  },
                                  "underwritingData": {
                                    "subscriberId": "SUB-001",
                                    "agentCode": "AGT-123",
                                    "riskClassification": "STANDARD",
                                    "businessType": "COMMERCIAL"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00001"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.insuredData.name").value("Empresa SA"));
    }

    // --- PUT 404 ---

    @Test
    void updateGeneralInfo_whenFolioNotFound_returns404() throws Exception {
        // GIVEN
        when(updateGeneralInfoUseCase.updateGeneralInfo(any()))
                .thenThrow(new FolioNotFoundException("FOL-UNKNOWN"));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-UNKNOWN/general-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "insuredData": {
                                    "name": "Empresa SA",
                                    "rfc": "EMP123456ABC",
                                    "email": "empresa@test.com",
                                    "phone": "5551234567"
                                  },
                                  "underwritingData": {
                                    "subscriberId": "SUB-001",
                                    "agentCode": "AGT-123",
                                    "riskClassification": "STANDARD",
                                    "businessType": "COMMERCIAL"
                                  },
                                  "version": 0
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    // --- PUT 409 version conflict ---

    @Test
    void updateGeneralInfo_whenVersionConflict_returns409() throws Exception {
        // GIVEN
        when(updateGeneralInfoUseCase.updateGeneralInfo(any()))
                .thenThrow(new VersionConflictException("FOL-2026-00001", 1L, 3L));

        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00001/general-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "insuredData": {
                                    "name": "Empresa SA",
                                    "rfc": "EMP123456ABC",
                                    "email": "empresa@test.com",
                                    "phone": "5551234567"
                                  },
                                  "underwritingData": {
                                    "subscriberId": "SUB-001",
                                    "agentCode": "AGT-123",
                                    "riskClassification": "STANDARD",
                                    "businessType": "COMMERCIAL"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    // --- PUT 422 validation error ---

    @Test
    void updateGeneralInfo_whenNameMissing_returns422() throws Exception {
        // WHEN / THEN
        mockMvc.perform(put("/v1/quotes/FOL-2026-00001/general-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "insuredData": {
                                    "rfc": "EMP123456ABC",
                                    "email": "empresa@test.com",
                                    "phone": "5551234567"
                                  },
                                  "underwritingData": {
                                    "subscriberId": "SUB-001",
                                    "agentCode": "AGT-123",
                                    "riskClassification": "STANDARD",
                                    "businessType": "COMMERCIAL"
                                  },
                                  "version": 1
                                }
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
