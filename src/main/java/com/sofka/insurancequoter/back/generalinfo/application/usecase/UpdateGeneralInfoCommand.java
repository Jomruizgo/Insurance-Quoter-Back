package com.sofka.insurancequoter.back.generalinfo.application.usecase;

import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;

// Carries all data required to update general information of a quote
public record UpdateGeneralInfoCommand(
        String folioNumber,
        InsuredData insuredData,
        UnderwritingInfo underwritingInfo,
        Long version
) {}
