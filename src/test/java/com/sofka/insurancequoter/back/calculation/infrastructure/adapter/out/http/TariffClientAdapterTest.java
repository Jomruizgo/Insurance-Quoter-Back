package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.adapter.TariffClientAdapter;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tests for TariffClientAdapter using WireMock
class TariffClientAdapterTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TariffClientAdapter buildAdapter() {
        String baseUrl = "http://localhost:" + wireMock.getPort();
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        return new TariffClientAdapter(restClient);
    }

    private static final String TARIFF_JSON = """
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
            """;

    // --- Happy path: all 15 fields returned correctly ---

    @Test
    void fetchTariffs_returnsAllFields_onSuccess() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/tariffs"))
                .willReturn(okJson(TARIFF_JSON)));

        // WHEN
        Tariff result = buildAdapter().fetchTariffs();

        // THEN
        assertThat(result.fireRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0015));
        assertThat(result.fireContentsRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0012));
        assertThat(result.coverageExtensionFactor()).isEqualByComparingTo(BigDecimal.valueOf(0.07));
        assertThat(result.cattevFactor()).isEqualByComparingTo(BigDecimal.valueOf(0.0008));
        assertThat(result.catfhmFactor()).isEqualByComparingTo(BigDecimal.valueOf(0.0005));
        assertThat(result.debrisRemovalFactor()).isEqualByComparingTo(BigDecimal.valueOf(0.03));
        assertThat(result.extraordinaryExpensesFactor()).isEqualByComparingTo(BigDecimal.valueOf(0.02));
        assertThat(result.rentalLossRate()).isEqualByComparingTo(BigDecimal.valueOf(0.015));
        assertThat(result.businessInterruptionRate()).isEqualByComparingTo(BigDecimal.valueOf(0.015));
        assertThat(result.electronicEquipmentRate()).isEqualByComparingTo(BigDecimal.valueOf(0.002));
        assertThat(result.theftRate()).isEqualByComparingTo(BigDecimal.valueOf(0.003));
        assertThat(result.cashAndValuesRate()).isEqualByComparingTo(BigDecimal.valueOf(0.005));
        assertThat(result.glassRate()).isEqualByComparingTo(BigDecimal.valueOf(0.001));
        assertThat(result.luminousSignageRate()).isEqualByComparingTo(BigDecimal.valueOf(0.002));
        assertThat(result.commercialFactor()).isEqualByComparingTo(BigDecimal.valueOf(1.16));
    }

    // --- 5xx from core → CoreServiceException ---

    @Test
    void fetchTariffs_throwsCoreServiceException_onCoreError5xx() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/tariffs"))
                .willReturn(serverError()));

        // WHEN / THEN
        assertThatThrownBy(() -> buildAdapter().fetchTariffs())
                .isInstanceOf(CoreServiceException.class);
    }

    // --- Network timeout/connection reset → CoreServiceException ---

    @Test
    void fetchTariffs_throwsCoreServiceException_onCoreTimeout() {
        // GIVEN
        wireMock.stubFor(get(urlEqualTo("/v1/tariffs"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        // WHEN / THEN
        assertThatThrownBy(() -> buildAdapter().fetchTariffs())
                .isInstanceOf(CoreServiceException.class);
    }
}
