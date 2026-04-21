package com.sofka.insurancequoter.back.folio.domain.port.in;

import com.sofka.insurancequoter.back.folio.application.usecase.CreateFolioCommand;
import com.sofka.insurancequoter.back.folio.application.usecase.FolioCreationResult;

// Input port — defines the use case contract for folio creation
public interface CreateFolioUseCase {
    FolioCreationResult createFolio(CreateFolioCommand command);
}
