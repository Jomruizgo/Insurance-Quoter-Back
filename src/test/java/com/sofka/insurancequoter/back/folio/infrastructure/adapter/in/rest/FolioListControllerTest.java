package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;
import com.sofka.insurancequoter.back.folio.domain.port.in.CreateFolioUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioListRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioRestMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FolioListControllerTest {

    @Mock
    private ListFoliosUseCase listFoliosUseCase;

    @Mock
    private CreateFolioUseCase createFolioUseCase;

    private MockMvc mockMvc;

    private static final Instant NOW = Instant.parse("2026-04-20T10:00:00Z");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FolioController(
                        createFolioUseCase, listFoliosUseCase,
                        new FolioRestMapper(), new FolioListRestMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- Happy path: returns list with one folio ---

    @Test
    void shouldReturn200WithFolioList_whenFoliosExist() throws Exception {
        // GIVEN
        FolioSummary summary = new FolioSummary(
                "FOL-2026-00001", "Empresa Alfa SA de CV",
                "AGT-001", "Carlos López",
                "CREATED", 1, 10, null, NOW);
        when(listFoliosUseCase.listFolios()).thenReturn(List.of(summary));

        // WHEN / THEN
        mockMvc.perform(get("/v1/folios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folios").isArray())
                .andExpect(jsonPath("$.folios[0].folioNumber").value("FOL-2026-00001"))
                .andExpect(jsonPath("$.folios[0].client").value("Empresa Alfa SA de CV"))
                .andExpect(jsonPath("$.folios[0].agentCode").value("AGT-001"))
                .andExpect(jsonPath("$.folios[0].agentName").value("Carlos López"))
                .andExpect(jsonPath("$.folios[0].status").value("CREATED"))
                .andExpect(jsonPath("$.folios[0].locationCount").value(1))
                .andExpect(jsonPath("$.folios[0].completionPct").value(10))
                .andExpect(jsonPath("$.folios[0].commercialPremium").doesNotExist());
    }

    // --- Empty list ---

    @Test
    void shouldReturn200WithEmptyList_whenNoFoliosExist() throws Exception {
        // GIVEN
        when(listFoliosUseCase.listFolios()).thenReturn(List.of());

        // WHEN / THEN
        mockMvc.perform(get("/v1/folios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folios").isArray())
                .andExpect(jsonPath("$.folios").isEmpty());
    }
}
