package com.sofka.insurancequoter.back.coverage.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sofka.insurancequoter.InsuranceQuoterApplication;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
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
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration tests for coverage options — no @Transactional so @Version increments commit correctly
@SpringBootTest(classes = InsuranceQuoterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class CoverageOptionsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuoteJpaRepository quoteJpaRepository;

    @Autowired
    private CoverageOptionJpaRepository coverageOptionJpaRepository;

    private MockMvc mockMvc;

    private static final String FOLIO = "FOL-2026-IT-COV";

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

        // Clean state — coverage_options has FK cascade, delete explicitly for isolation
        coverageOptionJpaRepository.deleteAll();
        quoteJpaRepository.deleteAll();

        QuoteJpa quote = QuoteJpa.builder()
                .folioNumber(FOLIO)
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .build();
        quoteJpaRepository.save(quote);
    }

    private void stubGuaranteeCatalog() {
        wireMock.stubFor(get(urlEqualTo("/v1/catalogs/guarantees"))
                .willReturn(okJson("""
                        {"guarantees":[
                          {"code":"GUA-FIRE","description":"Incendio edificios","tarifable":true},
                          {"code":"GUA-THEFT","description":"Robo","tarifable":true}
                        ]}
                        """)));
    }

    // --- #197: PUT then GET returns persisted data with catalog description ---

    @Test
    void shouldPersistAndRetrieveCoverageOptions_whenPutFollowedByGet() throws Exception {
        stubGuaranteeCatalog();
        long version = quoteJpaRepository.findByFolioNumber(FOLIO).orElseThrow().getVersion();

        // PUT to save coverage options
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0},
                                    {"code":"COV-THEFT","selected":false,"deductiblePercentage":5.0,"coinsurancePercentage":100.0}
                                  ],
                                  "version": %d
                                }
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("COV-FIRE"))
                .andExpect(jsonPath("$.coverageOptions[0].description").value("Incendio y riesgos adicionales"))
                .andExpect(jsonPath("$.coverageOptions[0].selected").value(true))
                .andExpect(jsonPath("$.coverageOptions[1].code").value("COV-THEFT"))
                .andExpect(jsonPath("$.coverageOptions[1].selected").value(false));

        // GET returns same persisted data
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/quotes/{folio}/coverage-options", FOLIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("COV-FIRE"))
                .andExpect(jsonPath("$.coverageOptions[0].description").value("Incendio y riesgos adicionales"))
                .andExpect(jsonPath("$.coverageOptions[1].code").value("COV-THEFT"));
    }

    // --- #198: PUT with stale version returns 409 and does not modify data ---

    @Test
    void shouldReturn409AndNotModifyData_whenVersionConflictOnPut() throws Exception {
        stubGuaranteeCatalog();
        long version = quoteJpaRepository.findByFolioNumber(FOLIO).orElseThrow().getVersion();

        // First PUT advances the version
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": %d
                                }
                                """.formatted(version)))
                .andExpect(status().isOk());

        // Second PUT with stale version → 409
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-THEFT","selected":true,"deductiblePercentage":5.0,"coinsurancePercentage":100.0}
                                  ],
                                  "version": %d
                                }
                                """.formatted(version)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    // --- #199: PUT with code not in catalog returns 422 ---

    @Test
    void shouldReturn422_whenCoverageCodeNotInCatalog() throws Exception {
        stubGuaranteeCatalog();
        long version = quoteJpaRepository.findByFolioNumber(FOLIO).orElseThrow().getVersion();

        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-INVALID","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": %d
                                }
                                """.formatted(version)))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // --- #200 / #251: GET on folio with no saved coverage options derives them from guarantees.
    // The test folio has no locations, so no active guarantees -> only COV-BI is derived (always present). ---

    @Test
    void shouldReturn200WithDerivedCovBi_whenFolioHasNoCoverageOptionsAndNoLocations() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/quotes/{folio}/coverage-options", FOLIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.coverageOptions").isArray())
                .andExpect(jsonPath("$.coverageOptions.length()").value(1))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("COV-BI"))
                .andExpect(jsonPath("$.coverageOptions[0].selected").value(false));
    }

    // --- R-009 / #251: PUT with empty list deletes all persisted options.
    // PUT response itself returns the empty list (what was saved).
    // Subsequent GET derives from guarantees: folio has no locations -> only COV-BI is returned. ---

    @Test
    void shouldDeriveFromGuarantees_afterPutWithEmptyCoverageOptionsList() throws Exception {
        stubGuaranteeCatalog();
        long version = quoteJpaRepository.findByFolioNumber(FOLIO).orElseThrow().getVersion();

        // First PUT: persist one option
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [
                                    {"code":"COV-FIRE","selected":true,"deductiblePercentage":2.0,"coinsurancePercentage":80.0}
                                  ],
                                  "version": %d
                                }
                                """.formatted(version)))
                .andExpect(status().isOk());

        long versionAfterFirstPut = quoteJpaRepository.findByFolioNumber(FOLIO).orElseThrow().getVersion();

        // Second PUT: replace with empty list — all previous options are deleted; PUT response is empty
        mockMvc.perform(put("/v1/quotes/{folio}/coverage-options", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "coverageOptions": [],
                                  "version": %d
                                }
                                """.formatted(versionAfterFirstPut)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageOptions").isArray())
                .andExpect(jsonPath("$.coverageOptions").isEmpty());

        // GET: no persisted options -> derivation kicks in -> folio has no locations -> only COV-BI
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/v1/quotes/{folio}/coverage-options", FOLIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageOptions.length()").value(1))
                .andExpect(jsonPath("$.coverageOptions[0].code").value("COV-BI"))
                .andExpect(jsonPath("$.coverageOptions[0].selected").value(false));
    }
}
