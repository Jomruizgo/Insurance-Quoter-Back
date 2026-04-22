package com.sofka.insurancequoter.back.location.domain.port.out;

import java.time.Instant;

// Output port for reading and updating the optimistic lock version on a quote
public interface QuoteVersionRepository {

    // Returns the current version; throws FolioNotFoundException if folio does not exist
    Long findVersionByFolioNumber(String folioNumber);

    void incrementVersion(String folioNumber);

    Instant getUpdatedAt(String folioNumber);
}
