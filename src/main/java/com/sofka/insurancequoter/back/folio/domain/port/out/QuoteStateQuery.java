package com.sofka.insurancequoter.back.folio.domain.port.out;

import com.sofka.insurancequoter.back.folio.domain.model.QuoteSnapshot;

// Read-only projection port: returns the data needed to evaluate quote state sections
public interface QuoteStateQuery {
    QuoteSnapshot findByFolioNumber(String folioNumber);
}
