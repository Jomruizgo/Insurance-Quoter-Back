package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.AcceptQuoteCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.application.usecase.exception.CalculationResultNotFoundException;
import com.sofka.insurancequoter.back.calculation.domain.model.AcceptQuoteResult;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.AcceptQuoteUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.in.CalculatePremiumUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.in.GetCalculationResultUseCase;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request.AcceptQuoteRequest;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request.CalculatePremiumRequest;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.AcceptQuoteResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CalculationResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.mapper.CalculationRestMapper;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.swaggerdocs.CalculationApi;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// REST controller for the Calculation bounded context — delegates entirely to use cases
@RestController
public class CalculationController implements CalculationApi {

    private final CalculatePremiumUseCase calculatePremiumUseCase;
    private final GetCalculationResultUseCase getCalculationResultUseCase;
    private final AcceptQuoteUseCase acceptQuoteUseCase;
    private final CalculationRestMapper calculationRestMapper;

    public CalculationController(CalculatePremiumUseCase calculatePremiumUseCase,
                                 GetCalculationResultUseCase getCalculationResultUseCase,
                                 AcceptQuoteUseCase acceptQuoteUseCase,
                                 CalculationRestMapper calculationRestMapper) {
        this.calculatePremiumUseCase = calculatePremiumUseCase;
        this.getCalculationResultUseCase = getCalculationResultUseCase;
        this.acceptQuoteUseCase = acceptQuoteUseCase;
        this.calculationRestMapper = calculationRestMapper;
    }

    @Override
    public ResponseEntity<CalculationResponse> calculate(
            @PathVariable String folio,
            @Valid @RequestBody CalculatePremiumRequest request) {
        CalculatePremiumCommand command = new CalculatePremiumCommand(folio, request.version());
        CalculationResult result = calculatePremiumUseCase.calculate(command);
        return ResponseEntity.ok(calculationRestMapper.toResponse(result));
    }

    @Override
    public ResponseEntity<CalculationResponse> getCalculationResult(@PathVariable String folio) {
        try {
            CalculationResult result = getCalculationResultUseCase.get(folio);
            return ResponseEntity.ok(calculationRestMapper.toResponse(result));
        } catch (CalculationResultNotFoundException e) {
            return ResponseEntity.noContent().build();
        }
    }

    @Override
    public ResponseEntity<AcceptQuoteResponse> accept(
            @PathVariable String folio,
            @Valid @RequestBody AcceptQuoteRequest request) {
        AcceptQuoteCommand command = new AcceptQuoteCommand(folio, request.acceptedBy(), request.version());
        AcceptQuoteResult result = acceptQuoteUseCase.accept(command);
        return ResponseEntity.ok(new AcceptQuoteResponse(
                result.folioNumber(),
                result.quoteStatus(),
                result.acceptedBy(),
                result.acceptedAt(),
                result.version()
        ));
    }
}
