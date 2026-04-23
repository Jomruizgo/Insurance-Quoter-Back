package com.sofka.insurancequoter.back.generalinfo.infrastructure.config;

import com.sofka.insurancequoter.back.generalinfo.application.usecase.GetGeneralInfoUseCaseImpl;
import com.sofka.insurancequoter.back.generalinfo.application.usecase.UpdateGeneralInfoUseCaseImpl;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.GetGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.UpdateGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Wires the general-info use cases with their output port implementations
@Configuration
public class GeneralInfoConfig {

    @Bean
    public GetGeneralInfoUseCase getGeneralInfoUseCase(GeneralInfoRepository generalInfoRepository) {
        return new GetGeneralInfoUseCaseImpl(generalInfoRepository);
    }

    @Bean
    public UpdateGeneralInfoUseCase updateGeneralInfoUseCase(GeneralInfoRepository generalInfoRepository) {
        return new UpdateGeneralInfoUseCaseImpl(generalInfoRepository);
    }
}
