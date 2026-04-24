package com.sofka.insurancequoter.back.coverage.infrastructure.config;

import com.sofka.insurancequoter.back.coverage.application.usecase.GetCoverageOptionsUseCaseImpl;
import com.sofka.insurancequoter.back.coverage.application.usecase.SaveCoverageOptionsUseCaseImpl;
import com.sofka.insurancequoter.back.coverage.domain.port.in.GetCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.in.SaveCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.out.ActiveGuaranteeReader;
import com.sofka.insurancequoter.back.coverage.domain.port.out.GuaranteeCatalogClient;
import com.sofka.insurancequoter.back.coverage.domain.service.CoverageDerivationService;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.mapper.CoverageRestMapper;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.http.adapter.GuaranteeCatalogClientAdapter;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.adapter.ActiveGuaranteeJpaAdapter;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.adapter.CoverageOptionJpaAdapter;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.mappers.CoverageOptionPersistenceMapper;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

// Wires coverage bounded context: use cases, adapters, HTTP client
@Configuration
public class CoverageConfig {

    @Bean
    public GuaranteeCatalogClient guaranteeCatalogClient(
            @Value("${core.service.base-url:http://localhost:8081}") String baseUrl) {
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).build();
        return new GuaranteeCatalogClientAdapter(restClient);
    }

    @Bean
    public CoverageOptionPersistenceMapper coverageOptionPersistenceMapper() {
        return new CoverageOptionPersistenceMapper();
    }

    @Bean
    public CoverageOptionJpaAdapter coverageOptionJpaAdapter(
            CoverageOptionJpaRepository coverageOptionJpaRepository,
            QuoteJpaRepository quoteJpaRepository,
            CoverageOptionPersistenceMapper mapper) {
        return new CoverageOptionJpaAdapter(coverageOptionJpaRepository, quoteJpaRepository, mapper);
    }

    @Bean
    public CoverageDerivationService coverageDerivationService() {
        return new CoverageDerivationService();
    }

    @Bean
    public ActiveGuaranteeReader activeGuaranteeReader(
            QuoteJpaRepository quoteJpaRepository,
            LocationJpaRepository locationJpaRepository) {
        return new ActiveGuaranteeJpaAdapter(quoteJpaRepository, locationJpaRepository);
    }

    @Bean
    public GetCoverageOptionsUseCase getCoverageOptionsUseCase(
            CoverageOptionJpaAdapter adapter,
            ActiveGuaranteeReader activeGuaranteeReader,
            CoverageDerivationService coverageDerivationService) {
        return new GetCoverageOptionsUseCaseImpl(adapter, adapter, activeGuaranteeReader, coverageDerivationService);
    }

    @Bean
    public SaveCoverageOptionsUseCase saveCoverageOptionsUseCase(
            CoverageOptionJpaAdapter adapter,
            CoverageDerivationService coverageDerivationService) {
        return new SaveCoverageOptionsUseCaseImpl(adapter, adapter, coverageDerivationService);
    }

    @Bean
    public CoverageRestMapper coverageRestMapper() {
        return new CoverageRestMapper();
    }
}
