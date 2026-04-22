package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.*;
import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.domain.port.out.LocationStateReader;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteStateQuery;

public class GetQuoteStateUseCaseImpl implements GetQuoteStateUseCase {

    private final QuoteStateQuery quoteStateQuery;
    private final LocationStateReader locationStateReader;

    public GetQuoteStateUseCaseImpl(QuoteStateQuery quoteStateQuery,
                                    LocationStateReader locationStateReader) {
        this.quoteStateQuery = quoteStateQuery;
        this.locationStateReader = locationStateReader;
    }

    @Override
    public QuoteState getState(String folioNumber) {
        QuoteSnapshot snapshot = quoteStateQuery.findByFolioNumber(folioNumber);
        LocationStateSummary locationSummary = locationStateReader.readByFolioNumber(folioNumber);

        QuoteSections sections = new QuoteSections(
                SectionStatus.PENDING,                      // generalInfo — requires SPEC-006
                evaluateLayout(snapshot),
                evaluateLocations(locationSummary),
                SectionStatus.PENDING,                      // coverageOptions — requires SPEC-007
                evaluateCalculation(snapshot.quoteStatus())
        );

        int percentage = computePercentage(snapshot.quoteStatus(), sections);

        return new QuoteState(
                snapshot.folioNumber(),
                snapshot.quoteStatus(),
                percentage,
                sections,
                snapshot.version(),
                snapshot.updatedAt()
        );
    }

    private SectionStatus evaluateLayout(QuoteSnapshot snapshot) {
        if (snapshot.numberOfLocations() == null) return SectionStatus.PENDING;
        if (snapshot.numberOfLocations() > 0 && snapshot.locationType() != null) return SectionStatus.COMPLETE;
        return SectionStatus.IN_PROGRESS;
    }

    private SectionStatus evaluateLocations(LocationStateSummary summary) {
        if (summary.total() == 0) return SectionStatus.PENDING;
        if (summary.incompleteCount() > 0) return SectionStatus.INCOMPLETE;
        if (summary.completeCount() == summary.total()) return SectionStatus.COMPLETE;
        return SectionStatus.IN_PROGRESS;
    }

    private SectionStatus evaluateCalculation(String quoteStatus) {
        return "CALCULATED".equals(quoteStatus) ? SectionStatus.COMPLETE : SectionStatus.PENDING;
    }

    private int computePercentage(String quoteStatus, QuoteSections sections) {
        if ("CALCULATED".equals(quoteStatus)) return 100;
        long complete = countComplete(sections);
        return (int) Math.round(complete * 100.0 / 5);
    }

    private long countComplete(QuoteSections sections) {
        long count = 0;
        if (sections.generalInfo() == SectionStatus.COMPLETE) count++;
        if (sections.layout() == SectionStatus.COMPLETE) count++;
        if (sections.locations() == SectionStatus.COMPLETE) count++;
        if (sections.coverageOptions() == SectionStatus.COMPLETE) count++;
        if (sections.calculation() == SectionStatus.COMPLETE) count++;
        return count;
    }
}
