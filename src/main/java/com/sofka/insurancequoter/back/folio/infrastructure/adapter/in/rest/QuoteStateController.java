package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.domain.port.in.GetQuoteStateUseCase;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.QuoteStateResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.QuoteStateRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.swaggerdocs.QuoteStateApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/quotes")
@RequiredArgsConstructor
public class QuoteStateController implements QuoteStateApi {

    private final GetQuoteStateUseCase getQuoteStateUseCase;
    private final QuoteStateRestMapper mapper;

    @Override
    @GetMapping("/{folioNumber}/state")
    public ResponseEntity<QuoteStateResponse> getState(@PathVariable String folioNumber) {
        return ResponseEntity.ok(mapper.toResponse(getQuoteStateUseCase.getState(folioNumber)));
    }
}
