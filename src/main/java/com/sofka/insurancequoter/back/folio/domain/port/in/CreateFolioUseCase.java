package com.sofka.insurancequoter.back.folio.domain.port.in;

import com.sofka.insurancequoter.back.folio.application.usecase.CreateFolioCommand;
import com.sofka.insurancequoter.back.folio.application.usecase.FolioCreationResult;
import org.springframework.transaction.annotation.Transactional;

// Input port — defines the use case contract for folio creation
public interface CreateFolioUseCase {
    @Transactional
    FolioCreationResult createFolio(CreateFolioCommand command);
}
