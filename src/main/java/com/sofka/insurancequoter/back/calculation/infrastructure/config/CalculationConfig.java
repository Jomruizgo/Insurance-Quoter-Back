package com.sofka.insurancequoter.back.calculation.infrastructure.config;

import com.sofka.insurancequoter.back.calculation.application.usecase.AcceptQuoteUseCaseImpl;
import com.sofka.insurancequoter.back.calculation.application.usecase.CalculatePremiumUseCaseImpl;
import com.sofka.insurancequoter.back.calculation.application.usecase.GetCalculationResultUseCaseImpl;
import com.sofka.insurancequoter.back.calculation.domain.port.in.AcceptQuoteUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.in.CalculatePremiumUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.in.GetCalculationResultUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.out.AcceptQuoteRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.CalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.GetCalculationResultRepository;
import com.sofka.insurancequoter.back.calculation.domain.port.out.QuoteCalculationReader;
import com.sofka.insurancequoter.back.calculation.domain.port.out.TariffClient;
import com.sofka.insurancequoter.back.calculation.domain.service.CalculationService;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.http.adapter.TariffClientAdapter;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.adapter.CalculationResultJpaAdapter;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.mappers.CalculationPersistenceMapper;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.CalculationResultJpaRepository;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.PremiumByLocationJpaRepository;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

// Wires the calculation bounded context: domain service, use case, persistence adapter, HTTP client
@Configuration
public class CalculationConfig {

    @Bean
    public CalculationService calculationService() {
        return new CalculationService();
    }

    @Bean
    public TariffClient tariffClient(
            ObservationRegistry observationRegistry,
            @Value("${core.service.base-url:http://localhost:8081}") String baseUrl,
            @Value("${core.service.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${core.service.read-timeout-ms:10000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .observationRegistry(observationRegistry)
                .build();
        return new TariffClientAdapter(restClient);
    }

    @Bean
    public CalculationPersistenceMapper calculationPersistenceMapper() {
        return new CalculationPersistenceMapper();
    }

    @Bean
    public CalculationResultJpaAdapter calculationResultJpaAdapter(
            QuoteJpaRepository quoteJpaRepository,
            LocationJpaRepository locationJpaRepository,
            CoverageOptionJpaRepository coverageOptionJpaRepository,
            CalculationResultJpaRepository calculationResultJpaRepository,
            PremiumByLocationJpaRepository premiumByLocationJpaRepository,
            CalculationPersistenceMapper calculationPersistenceMapper,
            LocationPersistenceMapper locationPersistenceMapper) {
        return new CalculationResultJpaAdapter(
                quoteJpaRepository,
                locationJpaRepository,
                coverageOptionJpaRepository,
                calculationResultJpaRepository,
                premiumByLocationJpaRepository,
                calculationPersistenceMapper,
                locationPersistenceMapper
        );
    }

    @Bean
    public CalculatePremiumUseCase calculatePremiumUseCase(
            CalculationResultJpaAdapter calculationResultJpaAdapter,
            TariffClient tariffClient,
            GuaranteeCatalogClient guaranteeCatalogClient,
            CalculationService calculationService,
            MeterRegistry meterRegistry) {
        return new CalculatePremiumUseCaseImpl(
                (QuoteCalculationReader) calculationResultJpaAdapter,
                (CalculationResultRepository) calculationResultJpaAdapter,
                tariffClient,
                guaranteeCatalogClient,
                calculationService,
                meterRegistry
        );
    }

    @Bean
    public GetCalculationResultUseCase getCalculationResultUseCase(
            CalculationResultJpaAdapter calculationResultJpaAdapter) {
        return new GetCalculationResultUseCaseImpl(
                (GetCalculationResultRepository) calculationResultJpaAdapter
        );
    }

    @Bean
    public AcceptQuoteUseCase acceptQuoteUseCase(
            CalculationResultJpaAdapter calculationResultJpaAdapter,
            MeterRegistry meterRegistry) {
        return new AcceptQuoteUseCaseImpl(
                (AcceptQuoteRepository) calculationResultJpaAdapter,
                meterRegistry
        );
    }
}
