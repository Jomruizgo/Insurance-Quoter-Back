package com.sofka.insurancequoter.back.observability;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.sofka.insurancequoter.InsuranceQuoterApplication;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = InsuranceQuoterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoints.web.exposure.include=*"
)
@Testcontainers
class ActuatorPrometheusIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static WireMockServer wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

    @LocalServerPort
    int port;

    RestClient client;

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
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void actuatorPrometheus_returns200_withJvmMetrics() {
        String body = client.get()
                .uri("/actuator/prometheus")
                .retrieve()
                .body(String.class);
        assertThat(body).contains("jvm_memory_used_bytes");
    }

    @Test
    void actuatorPrometheus_returns200_withHttpServerMetrics() {
        // Make a request first so the HTTP timer has at least one observation
        client.get().uri("/actuator/health").retrieve().toBodilessEntity();

        String body = client.get()
                .uri("/actuator/prometheus")
                .retrieve()
                .body(String.class);
        assertThat(body).contains("http_server_requests_seconds");
    }

    @Test
    void actuatorHealth_returns200_withDbUp() {
        String body = client.get()
                .uri("/actuator/health")
                .retrieve()
                .body(String.class);
        assertThat(body).contains("\"status\":\"UP\"");
    }
}
