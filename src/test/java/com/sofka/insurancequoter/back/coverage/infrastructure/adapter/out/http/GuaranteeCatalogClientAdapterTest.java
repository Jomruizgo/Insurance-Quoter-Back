package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http.adapter.GuaranteeCatalogClientAdapter;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import com.github.tomakehurst.wiremock.http.Fault;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tests for GuaranteeCatalogClientAdapter using WireMock — TDD RED phase
class GuaranteeCatalogClientAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private GuaranteeCatalogClientAdapter buildAdapter() {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        return new GuaranteeCatalogClientAdapter(restClient);
    }

    // --- #188: successful core response → returns list of GuaranteeDto ---

    @Test
    void shouldReturnGuaranteeDtoList_whenCoreRespondsSuccessfully() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/catalogs/guarantees"))
                .willReturn(okJson("""
                        {
                          "guarantees": [
                            {"code": "GUA-FIRE", "description": "Incendio edificios", "tarifable": true},
                            {"code": "GUA-THEFT", "description": "Robo", "tarifable": true}
                          ]
                        }
                        """)));

        // WHEN
        List<GuaranteeDto> result = buildAdapter().fetchGuarantees();

        // THEN
        assertThat(result).hasSize(2);
        assertThat(result.get(0).code()).isEqualTo("GUA-FIRE");
        assertThat(result.get(0).description()).isEqualTo("Incendio edificios");
        assertThat(result.get(1).code()).isEqualTo("GUA-THEFT");
        assertThat(result.get(1).description()).isEqualTo("Robo");
    }

    // --- #189: core returns 5xx → throws CoreServiceException ---

    @Test
    void shouldThrowCoreServiceException_whenCoreReturns5xxError() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/catalogs/guarantees"))
                .willReturn(serverError()));

        // WHEN / THEN
        assertThatThrownBy(() -> buildAdapter().fetchGuarantees())
                .isInstanceOf(CoreServiceException.class)
                .hasMessageContaining("Core service error");
    }

    // --- R-003: network error (connection reset) → throws CoreServiceException, not 500 ---

    @Test
    void shouldThrowCoreServiceException_whenCoreConnectionIsReset() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/catalogs/guarantees"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        // WHEN / THEN
        assertThatThrownBy(() -> buildAdapter().fetchGuarantees())
                .isInstanceOf(CoreServiceException.class)
                .hasMessageContaining("Core service unavailable");
    }
}
