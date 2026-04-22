package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http.adapter;

import com.sofka.insurancequoter.back.coverage.application.usecase.dto.GuaranteeDto;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http.dto.GuaranteeCatalogResponse;
import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.List;

// Calls core service GET /v1/catalogs/guarantees and returns the guarantee catalog
public class GuaranteeCatalogClientAdapter implements GuaranteeCatalogClient {

    private final RestClient restClient;

    public GuaranteeCatalogClientAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public List<GuaranteeDto> fetchGuarantees() {
        try {
            GuaranteeCatalogResponse response = restClient.get()
                    .uri("/v1/catalogs/guarantees")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new CoreServiceException(
                                "Core service error fetching guarantees: HTTP " + res.getStatusCode().value());
                    })
                    .body(GuaranteeCatalogResponse.class);
            if (response == null || response.guarantees() == null) {
                return List.of();
            }
            return response.guarantees();
        } catch (CoreServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CoreServiceException("Core service unavailable: " + ex.getMessage());
        }
    }
}
