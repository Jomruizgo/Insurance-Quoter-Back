package com.sofka.insurancequoter.back.calculation.domain.port.out;

import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;

// Output port: reads the quote snapshot needed to perform a premium calculation
public interface QuoteCalculationReader {

    // Returns the snapshot for the given folio; throws FolioNotFoundException if not found
    QuoteCalculationSnapshot getSnapshot(String folioNumber);
}
