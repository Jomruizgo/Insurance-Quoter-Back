package com.sofka.insurancequoter.back.location.domain.port.out;

import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.util.List;

// Output port for reading and persisting location records per quote
public interface LocationRepository {

    List<Location> findActiveByQuoteId(Long quoteId);

    List<Location> findAllByQuoteId(Long quoteId);

    void insertAll(Long quoteId, List<Location> locations);

    void deactivateByIndices(Long quoteId, List<Integer> indices);

    void reactivateByIndices(Long quoteId, List<Integer> indices);
}
