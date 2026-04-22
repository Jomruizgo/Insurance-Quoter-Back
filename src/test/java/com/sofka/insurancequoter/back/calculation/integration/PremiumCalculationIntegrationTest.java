package com.sofka.insurancequoter.back.calculation.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sofka.insurancequoter.InsuranceQuoterApplication;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.CalculationResultJpaRepository;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for POST /v1/quotes/{folio}/calculate.
 * All tests are @Disabled pending end-to-end environment setup with Testcontainers + WireMock.
 * Enable them incrementally once the DB migrations are applied and the core service stub is ready.
 */
@Disabled("Integration skeleton — enable once DB and core service are available")
@SpringBootTest(classes = InsuranceQuoterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
@SuppressWarnings("rawtypes")
class PremiumCalculationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuoteJpaRepository quoteJpaRepository;

    @Autowired
    private LocationJpaRepository locationJpaRepository;

    @Autowired
    private CoverageOptionJpaRepository coverageOptionJpaRepository;

    @Autowired
    private CalculationResultJpaRepository calculationResultJpaRepository;

    private MockMvc mockMvc;

    private static final String FOLIO = "FOL-2026-IT-CALC";

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

        // Clean state — FK order: calculation_results → premiums_by_location, coverage_options, locations, quotes
        calculationResultJpaRepository.deleteAll();
        coverageOptionJpaRepository.deleteAll();
        locationJpaRepository.deleteAll();
        quoteJpaRepository.deleteAll();
    }

    // --- Stubs ---

    private void stubTariffs() {
        wireMock.stubFor(get(urlEqualTo("/v1/tariffs"))
                .willReturn(okJson("""
                        {
                          "tariffs": {
                            "fireRate": 0.0015,
                            "fireContentsRate": 0.0012,
                            "coverageExtensionFactor": 0.07,
                            "cattevFactor": 0.0008,
                            "catfhmFactor": 0.0005,
                            "debrisRemovalFactor": 0.03,
                            "extraordinaryExpensesFactor": 0.02,
                            "rentalLossRate": 0.015,
                            "businessInterruptionRate": 0.015,
                            "electronicEquipmentRate": 0.002,
                            "theftRate": 0.003,
                            "cashAndValuesRate": 0.005,
                            "glassRate": 0.001,
                            "luminousSignageRate": 0.002,
                            "commercialFactor": 1.16
                          }
                        }
                        """)));
    }

    private void stubGuarantees() {
        wireMock.stubFor(get(urlEqualTo("/v1/catalogs/guarantees"))
                .willReturn(okJson("""
                        {"guarantees":[
                          {"code":"GUA-FIRE","description":"Incendio edificios","tarifable":true},
                          {"code":"GUA-FIRE-CONT","description":"Incendio contenidos","tarifable":true},
                          {"code":"GUA-THEFT","description":"Robo","tarifable":true},
                          {"code":"GUA-ELEC","description":"Equipo electrónico","tarifable":true}
                        ]}
                        """)));
    }

    private QuoteJpa createQuote(String folio) {
        QuoteJpa quote = QuoteJpa.builder()
                .folioNumber(folio)
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .build();
        return quoteJpaRepository.save(quote);
    }

    // --- Test scenarios ---

    /**
     * Scenario 1: happy path — quote with one fully populated location
     * Expected: 200 with netPremium and commercialPremium calculated correctly
     */
    @Test
    void calculate_happyPath_returnsNetAndCommercialPremium() throws Exception {
        // GIVEN — quote + location with zipCode + fireKey + tarifable guarantee
        stubTariffs();
        stubGuarantees();
        QuoteJpa quote = createQuote(FOLIO);
        long version = quote.getVersion();

        // TODO: persist a fully populated Location and CoverageOption via JPA before calling POST

        // WHEN / THEN
        mockMvc.perform(post("/v1/quotes/{folio}/calculate", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": %d}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value(FOLIO))
                .andExpect(jsonPath("$.quoteStatus").value("CALCULATED"))
                .andExpect(jsonPath("$.netPremium").isNumber())
                .andExpect(jsonPath("$.commercialPremium").isNumber())
                .andExpect(jsonPath("$.version").isNumber());
    }

    /**
     * Scenario 2: partial calculability — one location calculable, one missing zipCode
     * Expected: 200 with premiumsByLocation[0].calculable=true and [1].calculable=false
     */
    @Test
    void calculate_partialCalculability_returnsBlockingAlertForNonCalculable() throws Exception {
        // GIVEN — two locations: one complete, one missing zipCode
        stubTariffs();
        stubGuarantees();
        QuoteJpa quote = createQuote(FOLIO);
        long version = quote.getVersion();

        // TODO: persist one complete and one incomplete Location via JPA

        // WHEN / THEN
        mockMvc.perform(post("/v1/quotes/{folio}/calculate", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": %d}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.premiumsByLocation[0].calculable").value(true))
                .andExpect(jsonPath("$.premiumsByLocation[1].calculable").value(false))
                .andExpect(jsonPath("$.premiumsByLocation[1].blockingAlerts").isArray());
    }

    /**
     * Scenario 3: all locations non-calculable
     * Expected: 422 NO_CALCULABLE_LOCATIONS
     */
    @Test
    void calculate_allLocationsNonCalculable_returns422() throws Exception {
        // GIVEN — location missing zipCode
        stubTariffs();
        stubGuarantees();
        QuoteJpa quote = createQuote(FOLIO);
        long version = quote.getVersion();

        // TODO: persist a Location missing zipCode via JPA

        // WHEN / THEN
        mockMvc.perform(post("/v1/quotes/{folio}/calculate", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": %d}
                                """.formatted(version)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("NO_CALCULABLE_LOCATIONS"));
    }

    /**
     * Scenario 4: folio not found
     * Expected: 404 FOLIO_NOT_FOUND
     */
    @Test
    void calculate_folioNotFound_returns404() throws Exception {
        // GIVEN — no quote persisted for this folio
        String unknownFolio = "FOL-2026-NOTEXIST";

        // WHEN / THEN
        mockMvc.perform(post("/v1/quotes/{folio}/calculate", unknownFolio)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 0}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FOLIO_NOT_FOUND"));
    }

    /**
     * Scenario 5: version conflict — stale version provided
     * Expected: 409 VERSION_CONFLICT
     */
    @Test
    void calculate_staleVersion_returns409VersionConflict() throws Exception {
        // GIVEN — quote with version 0; request sends version 99 (stale)
        stubTariffs();
        stubGuarantees();
        createQuote(FOLIO);

        // WHEN / THEN — sending wrong version triggers VersionConflictException
        mockMvc.perform(post("/v1/quotes/{folio}/calculate", FOLIO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version": 99}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }
}
