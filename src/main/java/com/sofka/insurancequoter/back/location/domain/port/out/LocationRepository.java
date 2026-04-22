package com.sofka.insurancequoter.back.location.domain.port.out;

import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.LocationSummary;

import java.util.List;

// Output port for reading and persisting location records per quote
public interface LocationRepository {

    List<Location> findActiveByQuoteId(Long quoteId);

    List<Location> findAllByQuoteId(Long quoteId);

    void insertAll(Long quoteId, List<Location> locations);

    void deactivateByIndices(Long quoteId, List<Integer> indices);

    void reactivateByIndices(Long quoteId, List<Integer> indices);

    // --- location management ---

    List<Location> findByFolioNumber(String folioNumber);

    List<Location> replaceAll(String folioNumber, List<Location> locations);

    Location patchOne(String folioNumber, int index, Location mergedLocation);

    List<LocationSummary> findSummaryByFolioNumber(String folioNumber);

    boolean existsByFolioAndIndex(String folioNumber, int index);
}
