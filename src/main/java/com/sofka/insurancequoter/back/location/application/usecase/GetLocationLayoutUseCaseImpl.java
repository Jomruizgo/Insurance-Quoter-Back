package com.sofka.insurancequoter.back.location.application.usecase;

import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.out.QuoteLayoutRepository;

// Retrieves the layout configuration of a quote identified by its folio number
public class GetLocationLayoutUseCaseImpl implements GetLocationLayoutUseCase {

    private final QuoteLayoutRepository quoteLayoutRepository;

    public GetLocationLayoutUseCaseImpl(QuoteLayoutRepository quoteLayoutRepository) {
        this.quoteLayoutRepository = quoteLayoutRepository;
    }

    @Override
    public GetLayoutResult getLayout(String folioNumber) {
        QuoteLayoutData data = quoteLayoutRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));

        LocationType locationType = data.locationType() != null
                ? LocationType.valueOf(data.locationType())
                : null;

        LayoutConfiguration layout = new LayoutConfiguration(data.numberOfLocations(), locationType);

        return new GetLayoutResult(data.folioNumber(), layout, data.version());
    }
}
