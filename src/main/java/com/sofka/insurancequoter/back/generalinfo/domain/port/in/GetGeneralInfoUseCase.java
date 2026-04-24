package com.sofka.insurancequoter.back.generalinfo.domain.port.in;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;

// Input port: retrieve the general information of a quote by folio number
public interface GetGeneralInfoUseCase {
    GeneralInfo getGeneralInfo(String folioNumber);
}
