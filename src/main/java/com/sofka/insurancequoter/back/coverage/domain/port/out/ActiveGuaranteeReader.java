package com.sofka.insurancequoter.back.coverage.domain.port.out;

import java.util.List;

// Output port: reads active guarantee information from location data for a given folio
public interface ActiveGuaranteeReader {

    // Returns guarantee codes whose insuredValue > 0 across all locations of the folio
    List<String> readActiveGuaranteeCodes(String folioNumber);

    // Returns true if any location of the folio has a non-null, non-blank catastrophicZone
    boolean hasCatastrophicZone(String folioNumber);
}
