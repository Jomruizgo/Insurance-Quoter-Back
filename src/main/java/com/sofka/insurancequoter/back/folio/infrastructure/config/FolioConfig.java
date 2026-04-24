package com.sofka.insurancequoter.back.folio.infrastructure.config;

import com.sofka.insurancequoter.back.folio.application.usecase.CreateFolioUseCaseImpl;
import com.sofka.insurancequoter.back.folio.application.usecase.GetQuoteStateUseCaseImpl;
import com.sofka.insurancequoter.back.folio.application.usecase.ListFoliosUseCaseImpl;
import com.sofka.insurancequoter.back.folio.domain.port.in.CreateFolioUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoverageOptionsStateReader;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;
import com.sofka.insurancequoter.back.folio.domain.port.out.LocationStateReader;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteRepository;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteStateQuery;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.http.adapter.CoreServiceClientAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class FolioConfig {

    @Bean
    public RestClient coreRestClient(@Value("${core.service.base-url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean
    public CoreServiceClient coreServiceClient(RestClient coreRestClient) {
        return new CoreServiceClientAdapter(coreRestClient);
    }

    @Bean
    public CreateFolioUseCase createFolioUseCase(QuoteRepository quoteRepository,
                                                  CoreServiceClient coreServiceClient) {
        return new CreateFolioUseCaseImpl(quoteRepository, coreServiceClient);
    }

    @Bean
    public GetQuoteStateUseCase getQuoteStateUseCase(QuoteStateQuery quoteStateQuery,
                                                      LocationStateReader locationStateReader,
                                                      CoverageOptionsStateReader coverageOptionsStateReader) {
        return new GetQuoteStateUseCaseImpl(quoteStateQuery, locationStateReader, coverageOptionsStateReader);
    }

    @Bean
    public ListFoliosUseCase listFoliosUseCase(FolioListQuery folioListQuery,
                                                GetQuoteStateUseCase getQuoteStateUseCase,
                                                CoreServiceClient coreServiceClient) {
        return new ListFoliosUseCaseImpl(folioListQuery, getQuoteStateUseCase, coreServiceClient);
    }
}
