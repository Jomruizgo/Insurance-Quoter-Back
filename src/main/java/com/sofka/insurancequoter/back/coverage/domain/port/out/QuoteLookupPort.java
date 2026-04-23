package com.sofka.insurancequoter.back.coverage.domain.port.out;

import java.time.Instant;

// Output port: verifies quote existence and manages optimistic locking version checks
public interface QuoteLookupPort {

    // Throws FolioNotFoundException if the folio does not exist
    void assertFolioExists(String folioNumber);

    // Returns the current optimistic-lock version of the quote
    long getCurrentVersion(String folioNumber);

    // Returns the last-updated timestamp of the quote
    Instant getUpdatedAt(String folioNumber);

    // Throws VersionConflictException if the stored version differs from expectedVersion
    void assertVersionMatches(String folioNumber, long expectedVersion);
}
