package com.sofka.insurancequoter.back.location.domain.port.out;

import com.sofka.insurancequoter.back.location.application.usecase.QuoteLayoutData;

import java.util.Optional;

// Output port for reading and persisting quote layout data
public interface QuoteLayoutRepository {

    Optional<QuoteLayoutData> findByFolioNumber(String folioNumber);

    QuoteLayoutData save(QuoteLayoutData data);
}
