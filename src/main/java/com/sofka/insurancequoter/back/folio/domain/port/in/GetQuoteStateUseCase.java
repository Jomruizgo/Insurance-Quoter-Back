package com.sofka.insurancequoter.back.folio.domain.port.in;

import com.sofka.insurancequoter.back.folio.domain.model.QuoteState;

public interface GetQuoteStateUseCase {
    QuoteState getState(String folioNumber);
}
