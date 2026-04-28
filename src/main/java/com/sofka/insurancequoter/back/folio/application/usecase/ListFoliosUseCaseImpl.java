package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.calculation.domain.port.in.GetCalculationResultUseCase;
import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;
import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.in.ListFoliosUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.CoreServiceClient;
import com.sofka.insurancequoter.back.folio.domain.port.out.FolioListQuery;

import java.math.BigDecimal;
import java.util.List;

// Use case: retrieves all folios and enriches each entry with agentName (from core),
// completionPct (from GetQuoteStateUseCase) and commercialPremium (from GetCalculationResultUseCase).
// Registered as @Bean in FolioConfig.
public class ListFoliosUseCaseImpl implements ListFoliosUseCase {

    private final FolioListQuery folioListQuery;
    private final GetQuoteStateUseCase getQuoteStateUseCase;
    private final CoreServiceClient coreServiceClient;
    private final GetCalculationResultUseCase getCalculationResultUseCase;

    public ListFoliosUseCaseImpl(FolioListQuery folioListQuery,
                                  GetQuoteStateUseCase getQuoteStateUseCase,
                                  CoreServiceClient coreServiceClient,
                                  GetCalculationResultUseCase getCalculationResultUseCase) {
        this.folioListQuery = folioListQuery;
        this.getQuoteStateUseCase = getQuoteStateUseCase;
        this.coreServiceClient = coreServiceClient;
        this.getCalculationResultUseCase = getCalculationResultUseCase;
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
        BigDecimal commercialPremium = resolveCommercialPremium(raw.folioNumber());

        return new FolioSummary(
                raw.folioNumber(),
                raw.client(),
                raw.agentCode(),
                agentName,
                raw.status(),
                raw.locationCount(),
                completionPct,
                commercialPremium,
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

    // Graceful degradation: if no calculation result exists yet, return null.
    private BigDecimal resolveCommercialPremium(String folioNumber) {
        try {
            return getCalculationResultUseCase.get(folioNumber).commercialPremium();
        } catch (Exception ex) {
            return null;
        }
    }
}
