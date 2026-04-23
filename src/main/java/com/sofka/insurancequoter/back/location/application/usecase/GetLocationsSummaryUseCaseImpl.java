package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LocationSummary;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationsSummaryUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.LocationRepository;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteVersionRepository;

import java.util.List;

// Returns a compact validation summary for all locations in a quote
public class GetLocationsSummaryUseCaseImpl implements GetLocationsSummaryUseCase {

    private final LocationRepository locationRepository;
    private final QuoteVersionRepository quoteVersionRepository;

    public GetLocationsSummaryUseCaseImpl(LocationRepository locationRepository,
                                          QuoteVersionRepository quoteVersionRepository) {
        this.locationRepository = locationRepository;
        this.quoteVersionRepository = quoteVersionRepository;
    }

    @Override
    public SummaryResult getSummary(String folioNumber) {
        quoteVersionRepository.findVersionByFolioNumber(folioNumber); // validates folio exists

        List<LocationSummary> summaries = locationRepository.findSummaryByFolioNumber(folioNumber);

        int total = summaries.size();
        int complete = (int) summaries.stream()
                .filter(s -> ValidationStatus.COMPLETE.equals(s.validationStatus()))
                .count();
        int incomplete = total - complete;

        return new SummaryResult(folioNumber, total, complete, incomplete, summaries);
    }
}
