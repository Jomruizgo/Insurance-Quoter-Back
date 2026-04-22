package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.adapter;

import com.sofka.insurancequoter.back.calculation.domain.model.Tariff;
import com.sofka.insurancequoter.back.calculation.domain.port.out.TariffClient;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.dto.TariffData;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.dto.TariffResponse;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

// Calls core service GET /v1/tariffs and maps response to the Tariff domain model
public class TariffClientAdapter implements TariffClient {

    private final RestClient restClient;

    public TariffClientAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Tariff fetchTariffs() {
        try {
            TariffResponse response = restClient.get()
                    .uri("/v1/tariffs")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CoreServiceException(
                                "Core service error fetching tariffs: HTTP " + res.getStatusCode().value());
                    })
                    .body(TariffResponse.class);

            if (response == null || response.tariffs() == null) {
                throw new CoreServiceException("Core service returned empty tariffs response");
            }

            TariffData data = response.tariffs();
            validateTariffFields(data);
            return new Tariff(
                    data.fireRate(),
                    data.fireContentsRate(),
                    data.coverageExtensionFactor(),
                    data.cattevFactor(),
                    data.catfhmFactor(),
                    data.debrisRemovalFactor(),
                    data.extraordinaryExpensesFactor(),
                    data.rentalLossRate(),
                    data.businessInterruptionRate(),
                    data.electronicEquipmentRate(),
                    data.theftRate(),
                    data.cashAndValuesRate(),
                    data.glassRate(),
                    data.luminousSignageRate(),
                    data.commercialFactor()
            );
        } catch (CoreServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CoreServiceException("Core service unavailable fetching tariffs: " + ex.getMessage());
        }
    }

    private void validateTariffFields(TariffData data) {
        if (data.fireRate() == null || data.fireContentsRate() == null ||
                data.coverageExtensionFactor() == null || data.cattevFactor() == null ||
                data.catfhmFactor() == null || data.debrisRemovalFactor() == null ||
                data.extraordinaryExpensesFactor() == null || data.rentalLossRate() == null ||
                data.businessInterruptionRate() == null || data.electronicEquipmentRate() == null ||
                data.theftRate() == null || data.cashAndValuesRate() == null ||
                data.glassRate() == null || data.luminousSignageRate() == null ||
                data.commercialFactor() == null) {
            throw new CoreServiceException("Core service returned incomplete tariff data: one or more rate fields are null");
        }
    }
}
