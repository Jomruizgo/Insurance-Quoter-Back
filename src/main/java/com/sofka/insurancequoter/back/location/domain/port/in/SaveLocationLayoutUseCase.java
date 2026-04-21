package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutCommand;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutResult;

// Input port: saves or updates the location layout for a given folio
public interface SaveLocationLayoutUseCase {

    SaveLayoutResult saveLayout(SaveLayoutCommand command);
}
