package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.adapter;

import com.sofka.insurancequoter.back.folio.application.usecase.CoreServiceException;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto.AgentsResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto.CoreFolioResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.dto.SubscribersResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

// Adapter: implements CoreServiceClient output port using Spring RestClient
@RequiredArgsConstructor
public class CoreServiceClientAdapter implements CoreServiceClient {

    private final RestClient restClient;

    @Override
    public boolean existsSubscriber(String subscriberId) {
        SubscribersResponse response = restClient.get()
                .uri("/v1/subscribers")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new CoreServiceException(
                            "Core service error fetching subscribers: HTTP " + res.getStatusCode().value());
                })
                .body(SubscribersResponse.class);
        if (response == null || response.subscribers() == null) {
            return false;
        }
        return response.subscribers().stream()
                .anyMatch(s -> subscriberId.equals(s.id()));
    }

    @Override
    public boolean existsAgent(String agentCode) {
        AgentsResponse response = restClient.get()
                .uri("/v1/agents")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new CoreServiceException(
                            "Core service error fetching agents: HTTP " + res.getStatusCode().value());
                })
                .body(AgentsResponse.class);
        if (response == null || response.agents() == null) {
            return false;
        }
        return response.agents().stream()
                .anyMatch(a -> agentCode.equals(a.code()));
    }

    @Override
    public String nextFolioNumber() {
        CoreFolioResponse response = restClient.get()
                .uri("/v1/folios")
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new CoreServiceException(
                            "Core service error fetching folio number: HTTP " + res.getStatusCode().value());
                })
                .body(CoreFolioResponse.class);
        if (response == null) {
            throw new CoreServiceException("Core service returned empty folio response");
        }
        return response.folioNumber();
    }
}
