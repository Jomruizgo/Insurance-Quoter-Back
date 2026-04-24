package com.sofka.insurancequoter.back.folio.domain.port.out;

import com.sofka.insurancequoter.back.folio.domain.model.SectionStatus;

public interface CoverageOptionsStateReader {
    SectionStatus readByFolioNumber(String folioNumber);
}
