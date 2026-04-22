package com.sofka.insurancequoter.back.folio.domain.port.out;

import com.sofka.insurancequoter.back.folio.domain.model.LocationStateSummary;

// Output port: aggregates location validation counts for a given folio
public interface LocationStateReader {
    LocationStateSummary readByFolioNumber(String folioNumber);
}
