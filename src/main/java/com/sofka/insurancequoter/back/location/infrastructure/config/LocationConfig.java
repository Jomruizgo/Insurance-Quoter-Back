package com.sofka.insurancequoter.back.location.infrastructure.config;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.*;
import com.sofka.insurancequoter.back.location.domain.port.in.*;
import com.sofka.insurancequoter.back.location.domain.port.out.*;
import com.sofka.insurancequoter.back.location.domain.service.LocationValidationService;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.http.adapter.ZipCodeValidationClientAdapter;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.adapter.QuoteVersionJpaAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Wires location management use cases with their output port adapters
@Configuration
public class LocationConfig {

    @Bean
    public LocationValidationService locationValidationService() {
        return new LocationValidationService();
    }

    @Bean
    public QuoteVersionRepository quoteVersionRepository(QuoteJpaRepository quoteJpaRepository) {
        return new QuoteVersionJpaAdapter(quoteJpaRepository);
    }

    @Bean
    public ZipCodeValidationClient zipCodeValidationClient(
            io.micrometer.observation.ObservationRegistry observationRegistry,
            @Value("${core.service.base-url:http://localhost:8081}") String baseUrl) {
        return new ZipCodeValidationClientAdapter(
                RestClient.builder()
                        .baseUrl(baseUrl)
                        .observationRegistry(observationRegistry)
                        .build()
        );
    }

    @Bean
    public GetLocationsUseCase getLocationsUseCase(LocationRepository locationRepository,
                                                   QuoteVersionRepository quoteVersionRepository) {
        return new GetLocationsUseCaseImpl(locationRepository, quoteVersionRepository);
    }

    @Bean
    public ReplaceLocationsUseCase replaceLocationsUseCase(LocationRepository locationRepository,
                                                           QuoteVersionRepository quoteVersionRepository,
                                                           ZipCodeValidationClient zipCodeValidationClient,
                                                           LocationValidationService locationValidationService) {
        return new ReplaceLocationsUseCaseImpl(locationRepository, quoteVersionRepository,
                zipCodeValidationClient, locationValidationService);
    }

    @Bean
    public PatchLocationUseCase patchLocationUseCase(LocationRepository locationRepository,
                                                     QuoteVersionRepository quoteVersionRepository,
                                                     ZipCodeValidationClient zipCodeValidationClient,
                                                     LocationValidationService locationValidationService) {
        return new PatchLocationUseCaseImpl(locationRepository, quoteVersionRepository,
                zipCodeValidationClient, locationValidationService);
    }

    @Bean
    public GetLocationsSummaryUseCase getLocationsSummaryUseCase(LocationRepository locationRepository,
                                                                  QuoteVersionRepository quoteVersionRepository) {
        return new GetLocationsSummaryUseCaseImpl(locationRepository, quoteVersionRepository);
    }
}
