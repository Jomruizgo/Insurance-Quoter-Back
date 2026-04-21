package com.sofka.insurancequoter.back.folio.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sofka.insurancequoter.InsuranceQuoterApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = InsuranceQuoterApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@Transactional
class CreateFolioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMock.stop();
    }

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("core.service.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        wireMock.resetAll();
    }

    private void stubCoreHappyPath(String folioNumber) {
        wireMock.stubFor(get(urlEqualTo("/v1/subscribers"))
                .willReturn(okJson("""
                        {"subscribers":[{"id":"SUB-001","name":"Test Subscriber"}]}
                        """)));
        wireMock.stubFor(get(urlEqualTo("/v1/agents"))
                .willReturn(okJson("""
                        {"agents":[{"code":"AGT-123","name":"Test Agent"}]}
                        """)));
        wireMock.stubFor(get(urlEqualTo("/v1/folios"))
                .willReturn(okJson("""
                        {"folioNumber":"%s"}
                        """.formatted(folioNumber))));
    }

    // --- CRITERIO-1.1: nuevo folio → 201 con folioNumber y status CREATED ---

    @Test
    void shouldReturn201_whenNewFolioCreated() throws Exception {
        // GIVEN
        stubCoreHappyPath("FOL-2026-IT-001");

        // WHEN / THEN
        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-IT-001"))
                .andExpect(jsonPath("$.quoteStatus").value("CREATED"))
                .andExpect(jsonPath("$.underwritingData.subscriberId").value("SUB-001"))
                .andExpect(jsonPath("$.underwritingData.agentCode").value("AGT-123"));
    }

    // --- CRITERIO-1.2: segunda llamada misma combinación → 200 idempotente ---

    @Test
    void shouldReturn200_whenFolioAlreadyExistsForSameSubscriberAndAgent() throws Exception {
        // GIVEN — first call creates the folio
        stubCoreHappyPath("FOL-2026-IT-002");
        mockMvc.perform(post("/v1/folios")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                        """));

        // WHEN — second call with same subscriber+agent
        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                                """))
                // THEN — idempotent: existing folio returned with 200
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-IT-002"))
                .andExpect(jsonPath("$.quoteStatus").value("CREATED"));
    }

    // --- CRITERIO-1.3: subscriber inválido → 400 ---

    @Test
    void shouldReturn400_whenSubscriberDoesNotExistInCore() throws Exception {
        // GIVEN — core returns empty subscriber list
        wireMock.stubFor(get(urlEqualTo("/v1/subscribers"))
                .willReturn(okJson("""
                        {"subscribers":[]}
                        """)));

        // WHEN / THEN
        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-UNKNOWN","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REFERENCE"));
    }

    // --- Error: core retorna 503 → 502 Bad Gateway ---

    @Test
    void shouldReturn502_whenCoreServiceIsUnavailable() throws Exception {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/subscribers"))
                .willReturn(serviceUnavailable()));

        // WHEN / THEN
        mockMvc.perform(post("/v1/folios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"subscriberId":"SUB-001","agentCode":"AGT-123"}
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CORE_SERVICE_ERROR"));
    }
}
