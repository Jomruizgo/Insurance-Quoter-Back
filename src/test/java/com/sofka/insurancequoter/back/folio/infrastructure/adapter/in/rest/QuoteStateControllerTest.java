package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.domain.model.*;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.QuoteStateResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.SectionsResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.QuoteStateRestMapper;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class QuoteStateControllerTest {

    @Mock
    private GetQuoteStateUseCase getQuoteStateUseCase;

    @Mock
    private QuoteStateRestMapper mapper;

    private MockMvc mockMvc;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    @BeforeEach
    void setUp() {
        var controller = new QuoteStateController(getQuoteStateUseCase, mapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getState_returns200WithCorrectBody() throws Exception {
        // GIVEN
        QuoteSections sections = new QuoteSections(
                SectionStatus.PENDING, SectionStatus.COMPLETE,
                SectionStatus.INCOMPLETE, SectionStatus.PENDING, SectionStatus.PENDING);
        QuoteState state = new QuoteState("FOL-001", "IN_PROGRESS", 20, sections, 3L, NOW);
        QuoteStateResponse response = new QuoteStateResponse(
                "FOL-001", "IN_PROGRESS", 20,
                new SectionsResponse("PENDING", "COMPLETE", "INCOMPLETE", "PENDING", "PENDING"),
                3L, NOW);

        when(getQuoteStateUseCase.getState("FOL-001")).thenReturn(state);
        when(mapper.toResponse(state)).thenReturn(response);

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/FOL-001/state"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-001"))
                .andExpect(jsonPath("$.quoteStatus").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completionPercentage").value(20))
                .andExpect(jsonPath("$.sections.layout").value("COMPLETE"))
                .andExpect(jsonPath("$.sections.locations").value("INCOMPLETE"))
                .andExpect(jsonPath("$.version").value(3));
    }

    @Test
    void getState_unknownFolio_returns404() throws Exception {
        // GIVEN
        when(getQuoteStateUseCase.getState("UNKNOWN"))
                .thenThrow(new FolioNotFoundException("UNKNOWN"));

        // WHEN / THEN
        mockMvc.perform(get("/v1/quotes/UNKNOWN/state"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }
}
