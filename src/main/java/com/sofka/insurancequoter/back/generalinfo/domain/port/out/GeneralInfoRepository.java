package com.sofka.insurancequoter.back.generalinfo.domain.port.out;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;

import java.util.Optional;

// Output port: persistence operations for general quote information
public interface GeneralInfoRepository {
    Optional<GeneralInfo> findByFolioNumber(String folioNumber);
    GeneralInfo save(GeneralInfo generalInfo);
}
