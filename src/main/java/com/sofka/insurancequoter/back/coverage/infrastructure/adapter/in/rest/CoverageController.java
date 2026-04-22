package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.coverage.application.usecase.command.SaveCoverageOptionsCommand;
import com.sofka.insurancequoter.back.coverage.application.usecase.dto.CoverageOptionsResponse;
import com.sofka.insurancequoter.back.coverage.domain.port.in.GetCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.domain.port.in.SaveCoverageOptionsUseCase;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request.SaveCoverageOptionsRequest;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response.CoverageOptionsListResponse;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.mapper.CoverageRestMapper;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.swaggerdocs.CoverageApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// REST controller for coverage options — delegates all logic to use cases
@RestController
public class CoverageController implements CoverageApi {

    private final GetCoverageOptionsUseCase getCoverageOptionsUseCase;
    private final SaveCoverageOptionsUseCase saveCoverageOptionsUseCase;
    private final CoverageRestMapper mapper;

    public CoverageController(GetCoverageOptionsUseCase getCoverageOptionsUseCase,
                               SaveCoverageOptionsUseCase saveCoverageOptionsUseCase,
                               CoverageRestMapper mapper) {
        this.getCoverageOptionsUseCase = getCoverageOptionsUseCase;
        this.saveCoverageOptionsUseCase = saveCoverageOptionsUseCase;
        this.mapper = mapper;
    }

    @Override
    public ResponseEntity<CoverageOptionsListResponse> getCoverageOptions(String folio) {
        CoverageOptionsResponse response = getCoverageOptionsUseCase.getCoverageOptions(folio);
        return ResponseEntity.ok(mapper.toListResponse(response));
    }

    @Override
    public ResponseEntity<CoverageOptionsListResponse> saveCoverageOptions(
            String folio, SaveCoverageOptionsRequest request) {
        SaveCoverageOptionsCommand command = mapper.toCommand(folio, request);
        CoverageOptionsResponse response = saveCoverageOptionsUseCase.saveCoverageOptions(command);
        return ResponseEntity.ok(mapper.toListResponse(response));
    }
}
