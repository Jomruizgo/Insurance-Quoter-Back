package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.folio.application.usecase.CreateFolioCommand;
import com.sofka.insurancequoter.back.folio.application.usecase.FolioCreationResult;
import com.sofka.insurancequoter.back.folio.domain.port.in.CreateFolioUseCase;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.CreateFolioRequest;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.FolioResponse;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.mapper.FolioRestMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.swaggerdocs.FolioApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// Parses HTTP, delegates to the use case, maps the result — no business logic here
@RestController
@RequiredArgsConstructor
public class FolioController implements FolioApi {

    private final CreateFolioUseCase createFolioUseCase;
    private final FolioRestMapper folioRestMapper;

    @Override
    public ResponseEntity<FolioResponse> createFolio(CreateFolioRequest request) {
        CreateFolioCommand command = new CreateFolioCommand(
                request.subscriberId(),
                request.agentCode()
        );
        FolioCreationResult result = createFolioUseCase.createFolio(command);
        FolioResponse response = folioRestMapper.toResponse(result.quote());
        return result.created()
                ? ResponseEntity.status(HttpStatus.CREATED).body(response)
                : ResponseEntity.ok(response);
    }
}
