package com.sofka.insurancequoter.back.generalinfo.domain.port.in;

import com.sofka.insurancequoter.back.generalinfo.application.usecase.UpdateGeneralInfoCommand;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;

// Input port: update the general information of a quote (optimistic locking enforced)
public interface UpdateGeneralInfoUseCase {
    GeneralInfo updateGeneralInfo(UpdateGeneralInfoCommand command);
}
