package com.sofka.insurancequoter.back.location.integration;

import com.sofka.insurancequoter.InsuranceQuoterApplication;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration tests without @Transactional so that @Version checks and commits happen correctly
@SpringBootTest(classes = InsuranceQuoterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Testcontainers
class SaveLocationLayoutIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private QuoteJpaRepository quoteJpaRepository;

    @Autowired
    private LocationJpaRepository locationJpaRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

        // Clean state between tests — locations first (FK), then quotes
        locationJpaRepository.deleteAll();
        quoteJpaRepository.deleteAll();

        QuoteJpa quote = QuoteJpa.builder()
                .folioNumber("FOL-2026-IT-001")
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .build();
        quoteJpaRepository.save(quote);
    }

    // CRITERIO-2.1: first save creates 3 empty locations
    @Test
    void shouldCreateLocations_whenSavingLayoutForFirstTime() throws Exception {
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "layoutConfiguration": {
                                    "numberOfLocations": 3,
                                    "locationType": "MULTIPLE"
                                  },
                                  "version": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(3))
                .andExpect(jsonPath("$.layoutConfiguration.locationType").value("MULTIPLE"));

        Long quoteId = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getId();
        assertThat(locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId)).hasSize(3);
    }

    // CRITERIO-2.2: increasing locations appends new empty records
    @Test
    void shouldAddLocations_whenIncreasingNumberOfLocations() throws Exception {
        // First save with 2
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":2,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isOk());

        // Reload version after first save
        long version = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getVersion();

        // Second save increasing to 4
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":4,"locationType":"MULTIPLE"},"version":%d}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(4));

        Long quoteId = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getId();
        assertThat(locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId)).hasSize(4);
    }

    // CRITERIO-2.3: reducing numberOfLocations marks excess as inactive
    @Test
    void shouldDeactivateLocations_whenReducingNumberOfLocations() throws Exception {
        // First save with 4
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":4,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isOk());

        long version = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getVersion();

        // Reduce to 2
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":2,"locationType":"MULTIPLE"},"version":%d}
                                """.formatted(version)))
                .andExpect(status().isOk());

        Long quoteId = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getId();
        // 2 active, 2 inactive — total 4 rows preserved
        assertThat(locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId)).hasSize(2);
        assertThat(locationJpaRepository.findAll()
                .stream().filter(l -> l.getQuoteId().equals(quoteId))).hasSize(4);
    }

    // CRITERIO-2.4: stale version returns 409
    @Test
    void shouldReturn409_whenVersionIsStale() throws Exception {
        // First save advances the version (0 → 1)
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":2,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isOk());

        // Send stale version 0 again — must conflict
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":3,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));
    }

    // R-005: reduce then increase must reactivate inactive rows, not violate UK constraint
    @Test
    void shouldReactivateLocations_whenIncreasingAfterPreviousReduction() throws Exception {
        // Save with 4
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":4,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isOk());

        long v1 = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getVersion();

        // Reduce to 2 (indices 3,4 become inactive)
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":2,"locationType":"MULTIPLE"},"version":%d}
                                """.formatted(v1)))
                .andExpect(status().isOk());

        long v2 = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getVersion();

        // Increase back to 4 — must reactivate 3,4, not insert duplicates
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":4,"locationType":"MULTIPLE"},"version":%d}
                                """.formatted(v2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(4));

        Long quoteId = quoteJpaRepository.findByFolioNumber("FOL-2026-IT-001").orElseThrow().getId();
        // 4 active, total rows still 4 (no duplicate inserts)
        assertThat(locationJpaRepository.findByQuoteIdAndActiveTrue(quoteId)).hasSize(4);
        assertThat(locationJpaRepository.findByQuoteId(quoteId)).hasSize(4);
    }

    // GET returns layout after save
    @Test
    void shouldReturnLayout_whenGetAfterSave() throws Exception {
        mockMvc.perform(put("/v1/quotes/FOL-2026-IT-001/locations/layout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"layoutConfiguration":{"numberOfLocations":3,"locationType":"MULTIPLE"},"version":0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v1/quotes/FOL-2026-IT-001/locations/layout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folioNumber").value("FOL-2026-IT-001"))
                .andExpect(jsonPath("$.layoutConfiguration.numberOfLocations").value(3))
                .andExpect(jsonPath("$.layoutConfiguration.locationType").value("MULTIPLE"));
    }
}
