package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.calculation.application.usecase.command.CalculatePremiumCommand;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.CalculatePremiumUseCase;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request.CalculatePremiumRequest;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CalculationResponse;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.mapper.CalculationRestMapper;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.swaggerdocs.CalculationApi;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// REST controller for the Calculation bounded context — delegates entirely to the use case
@RestController
public class CalculationController implements CalculationApi {

    private final CalculatePremiumUseCase calculatePremiumUseCase;
    private final CalculationRestMapper calculationRestMapper;

    public CalculationController(CalculatePremiumUseCase calculatePremiumUseCase,
                                 CalculationRestMapper calculationRestMapper) {
        this.calculatePremiumUseCase = calculatePremiumUseCase;
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
}
