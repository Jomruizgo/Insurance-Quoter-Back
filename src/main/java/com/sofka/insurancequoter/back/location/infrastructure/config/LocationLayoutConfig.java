package com.sofka.insurancequoter.back.location.infrastructure.config;

import com.sofka.insurancequoter.back.location.application.usecase.GetLocationLayoutUseCaseImpl;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLocationLayoutUseCaseImpl;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.in.SaveLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires the location layout use case implementations with their output port adapters
@Configuration
public class LocationLayoutConfig {

    @Bean
    public GetLocationLayoutUseCase getLocationLayoutUseCase(QuoteLayoutRepository quoteLayoutRepository) {
        return new GetLocationLayoutUseCaseImpl(quoteLayoutRepository);
    }

    @Bean
    public SaveLocationLayoutUseCase saveLocationLayoutUseCase(
            QuoteLayoutRepository quoteLayoutRepository,
            LocationRepository locationRepository) {
        return new SaveLocationLayoutUseCaseImpl(quoteLayoutRepository, locationRepository);
    }
}
