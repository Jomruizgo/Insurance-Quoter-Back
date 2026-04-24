package com.sofka.insurancequoter.back.generalinfo.application.usecase;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.UpdateGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;

// Updates general information of a quote, enforcing optimistic locking via version check
public class UpdateGeneralInfoUseCaseImpl implements UpdateGeneralInfoUseCase {

    private final GeneralInfoRepository generalInfoRepository;

    public UpdateGeneralInfoUseCaseImpl(GeneralInfoRepository generalInfoRepository) {
        this.generalInfoRepository = generalInfoRepository;
    }

    @Override
    public GeneralInfo updateGeneralInfo(UpdateGeneralInfoCommand command) {
        GeneralInfo existing = generalInfoRepository.findByFolioNumber(command.folioNumber())
                .orElseThrow(() -> new FolioNotFoundException(command.folioNumber()));

        if (!existing.version().equals(command.version())) {
            throw new VersionConflictException(command.folioNumber(), command.version(), existing.version());
        }

        GeneralInfo updated = new GeneralInfo(
                existing.folioNumber(),
                existing.quoteStatus(),
                command.insuredData(),
                command.underwritingInfo(),
                existing.updatedAt(),
                existing.version()
        );

        return generalInfoRepository.save(updated);
    }
}
