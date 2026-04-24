package com.sofka.insurancequoter.back.calculation.application.usecase;

import com.sofka.insurancequoter.back.calculation.application.usecase.exception.CalculationResultNotFoundException;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.port.in.GetCalculationResultUseCase;
import com.sofka.insurancequoter.back.calculation.domain.port.out.GetCalculationResultRepository;

public class GetCalculationResultUseCaseImpl implements GetCalculationResultUseCase {

    private final GetCalculationResultRepository repository;

    public GetCalculationResultUseCaseImpl(GetCalculationResultRepository repository) {
        this.repository = repository;
    }

    @Override
    public CalculationResult get(String folioNumber) {
        return repository.find(folioNumber)
                .orElseThrow(() -> new CalculationResultNotFoundException(folioNumber));
    }
}
