package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.http.adapter;

import com.sofka.insurancequoter.back.location.domain.model.ZipCodeInfo;
import com.sofka.insurancequoter.back.location.domain.port.out.ZipCodeValidationClient;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.http.dto.ZipCodeResponse;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

// Calls the core service to validate a zip code and return geographic enrichment data
public class ZipCodeValidationClientAdapter implements ZipCodeValidationClient {

    private final RestClient restClient;

    public ZipCodeValidationClientAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Optional<ZipCodeInfo> validate(String zipCode) {
        try {
            ZipCodeResponse response = restClient.get()
                    .uri("/v1/zip-codes/{zipCode}", zipCode)
                    .retrieve()
                    .body(ZipCodeResponse.class);

            if (response == null || Boolean.FALSE.equals(response.valid())) {
                return Optional.empty();
            }

            return Optional.of(new ZipCodeInfo(
                    response.zipCode(),
                    response.state(),
                    response.municipality(),
                    response.city(),
                    response.catastrophicZone(),
                    true
            ));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
