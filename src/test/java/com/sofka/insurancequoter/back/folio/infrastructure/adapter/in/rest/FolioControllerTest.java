package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.application.usecase.FolioCreationResult;
import com.sofka.insurancequoter.back.folio.application.usecase.InvalidReferenceException;
import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.domain.port.in.CreateFolioUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioListRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioRestMapper;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FolioControllerTest {

    @Mock
    private CreateFolioUseCase createFolioUseCase;

    @Mock
    private ListFoliosUseCase listFoliosUseCase;

    private MockMvc mockMvc;

    private static final Instant FIXED = Instant.parse("2026-04-20T14:30:00Z");

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new FolioController(
                        createFolioUseCase, listFoliosUseCase,
                        new FolioRestMapper(), new FolioListRestMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private Quote quote(String folio) {
        return new Quote(folio, QuoteStatus.CREATED, "SUB-001", "AGT-123", 1L, FIXED, FIXED);
    }

    // --- CRITERIO-1.1: creación nueva → HTTP 201 ---

    @Test
    void shouldReturn201_whenFolioIsCreatedSuccessfully() throws Exception {
        when(createFolioUseCase.createFolio(any()))
                .thenReturn(new FolioCreationResult(quote("FOL-2026-00042"), true));

        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00042"))
                .andExpect(jsonPath("$.quoteStatus").value("CREATED"))
                .andExpect(jsonPath("$.underwritingData.subscriberId").value("SUB-001"))
                .andExpect(jsonPath("$.underwritingData.agentCode").value("AGT-123"))
                .andExpect(jsonPath("$.version").value(1));
    }

    // --- CRITERIO-1.2: idempotencia → HTTP 200 ---

    @Test
    void shouldReturn200_whenFolioAlreadyExists() throws Exception {
        when(createFolioUseCase.createFolio(any()))
                .thenReturn(new FolioCreationResult(quote("FOL-2026-00042"), false));

        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-00042"));
    }

    // --- CRITERIO-1.4: campo faltante → HTTP 422 ---

    @Test
    void shouldReturn422_whenAgentCodeIsMissing() throws Exception {
        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001"}
                                """))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- CRITERIO-1.3: referencia inválida → HTTP 400 ---

    @Test
    void shouldReturn400_whenSubscriberOrAgentIsInvalid() throws Exception {
        when(createFolioUseCase.createFolio(any()))
                .thenThrow(new InvalidReferenceException("Subscriber not found: SUB-999"));

        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-999","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFERENCE"))
                .andExpect(jsonPath("$.error").value("Invalid subscriber or agent"));
    }
}
