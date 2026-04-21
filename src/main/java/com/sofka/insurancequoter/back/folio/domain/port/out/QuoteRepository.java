package com.sofka.insurancequoter.back.folio.domain.port.out;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;

import java.util.Optional;

// Output port — persistence contract for Quote aggregate
public interface QuoteRepository {
    Optional<Quote> findActiveBySubscriberAndAgent(String subscriberId, String agentCode);
    Quote save(Quote quote);
}
