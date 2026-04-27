package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;
import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;

import java.util.List;

// Use case: retrieves all folios and enriches each entry with agentName (from core)
// and completionPct (from GetQuoteStateUseCase). Registered as @Bean in FolioConfig.
public class ListFoliosUseCaseImpl implements ListFoliosUseCase {

    private final FolioListQuery folioListQuery;
    private final GetQuoteStateUseCase getQuoteStateUseCase;
    private final CoreServiceClient coreServiceClient;

    public ListFoliosUseCaseImpl(FolioListQuery folioListQuery,
                                  GetQuoteStateUseCase getQuoteStateUseCase,
                                  CoreServiceClient coreServiceClient) {
        this.folioListQuery = folioListQuery;
        this.getQuoteStateUseCase = getQuoteStateUseCase;
        this.coreServiceClient = coreServiceClient;
    }

    @Override
    @io.micrometer.observation.annotation.Observed(name = "folio.list")
    public List<FolioSummary> listFolios() {
        return folioListQuery.findAll().stream()
                .map(this::enrich)
                .toList();
    }

    private FolioSummary enrich(FolioRaw raw) {
        String agentName = resolveAgentName(raw.agentCode());
        int completionPct = resolveCompletionPct(raw.folioNumber());

        return new FolioSummary(
                raw.folioNumber(),
                raw.client(),
                raw.agentCode(),
                agentName,
                raw.status(),
                raw.locationCount(),
                completionPct,
                null,           // commercialPremium: not yet in schema
                raw.updatedAt()
        );
    }

    // Graceful degradation: if the core service is unavailable, agentName is null.
    private String resolveAgentName(String agentCode) {
        try {
            return coreServiceClient.getAgentName(agentCode);
        } catch (CoreServiceException ex) {
            return null;
        }
    }

    // Graceful degradation: if state cannot be computed (e.g. folio has no data yet), return 0.
    private int resolveCompletionPct(String folioNumber) {
        try {
            return getQuoteStateUseCase.getState(folioNumber).completionPercentage();
        } catch (Exception ex) {
            return 0;
        }
    }
}
