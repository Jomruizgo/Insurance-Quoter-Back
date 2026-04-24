package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.adapter.CoreServiceClientAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("java:S100")
class CoreServiceClientAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private CoreServiceClientAdapter buildAdapter() {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        return new CoreServiceClientAdapter(restClient);
    }

    // --- existsSubscriber ---

    @Test
    void shouldReturnTrue_whenCoreReturnsSubscriberWithMatchingId() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/subscribers"))
                .willReturn(okJson("""
                        {
                          "subscribers": [
                            { "id": "SUB-001", "name": "Seguros Sofka" },
                            { "id": "SUB-002", "name": "Aseguradora Norte" }
                          ]
                        }
                        """)));

        // WHEN
        boolean result = buildAdapter().existsSubscriber("SUB-001");

        // THEN
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenCoreDoesNotReturnSubscriberWithMatchingId() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/subscribers"))
                .willReturn(okJson("""
                        {
                          "subscribers": [
                            { "id": "SUB-001", "name": "Seguros Sofka" }
                          ]
                        }
                        """)));

        // WHEN
        boolean result = buildAdapter().existsSubscriber("SUB-999");

        // THEN
        assertThat(result).isFalse();
    }

    // --- existsAgent ---

    @Test
    void shouldReturnTrue_whenCoreReturnsAgentWithMatchingCode() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/agents"))
                .willReturn(okJson("""
                        {
                          "agents": [
                            { "code": "AGT-123", "name": "Juan Pérez", "subscriberId": "SUB-001" }
                          ]
                        }
                        """)));

        // WHEN
        boolean result = buildAdapter().existsAgent("AGT-123");

        // THEN
        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenCoreDoesNotReturnAgentWithMatchingCode() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/agents"))
                .willReturn(okJson("""
                        {
                          "agents": [
                            { "code": "AGT-123", "name": "Juan Pérez", "subscriberId": "SUB-001" }
                          ]
                        }
                        """)));

        // WHEN
        boolean result = buildAdapter().existsAgent("AGT-999");

        // THEN
        assertThat(result).isFalse();
    }

    // --- nextFolioNumber ---

    @Test
    void shouldReturnFolioNumber_whenCoreRespondsWithFolioData() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/folios"))
                .willReturn(okJson("""
                        {
                          "folioNumber": "FOL-2026-00042",
                          "generatedAt": "2026-04-20T14:30:00Z"
                        }
                        """)));

        // WHEN
        String folioNumber = buildAdapter().nextFolioNumber();

        // THEN
        assertThat(folioNumber).isEqualTo("FOL-2026-00042");
    }
}
