package com.sofka.insurancequoter.back.generalinfo.application.usecase;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.GetGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;

// Retrieves the general information of a quote; throws FolioNotFoundException if the folio does not exist
public class GetGeneralInfoUseCaseImpl implements GetGeneralInfoUseCase {

    private final GeneralInfoRepository generalInfoRepository;

    public GetGeneralInfoUseCaseImpl(GeneralInfoRepository generalInfoRepository) {
        this.generalInfoRepository = generalInfoRepository;
    }

    @Override
    public GeneralInfo getGeneralInfo(String folioNumber) {
        return generalInfoRepository.findByFolioNumber(folioNumber)
                .orElseThrow(() -> new FolioNotFoundException(folioNumber));
    }
}
