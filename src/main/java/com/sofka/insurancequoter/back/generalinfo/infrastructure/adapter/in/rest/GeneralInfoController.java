package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.GetGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.domain.port.in.UpdateGeneralInfoUseCase;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.GeneralInfoResponse;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.UpdateGeneralInfoRequest;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.mapper.GeneralInfoRestMapper;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.swaggerdocs.GeneralInfoApi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

// Parses HTTP, delegates to the use cases, maps results - no business logic here
@RestController
@RequiredArgsConstructor
public class GeneralInfoController implements GeneralInfoApi {

    private final GetGeneralInfoUseCase getGeneralInfoUseCase;
    private final UpdateGeneralInfoUseCase updateGeneralInfoUseCase;
    private final GeneralInfoRestMapper generalInfoRestMapper;

    @Override
    public ResponseEntity<GeneralInfoResponse> getGeneralInfo(String folio) {
        GeneralInfo generalInfo = getGeneralInfoUseCase.getGeneralInfo(folio);
        return ResponseEntity.ok(generalInfoRestMapper.toResponse(generalInfo));
    }

    @Override
    public ResponseEntity<GeneralInfoResponse> updateGeneralInfo(String folio, UpdateGeneralInfoRequest request) {
        GeneralInfo updated = updateGeneralInfoUseCase.updateGeneralInfo(
                generalInfoRestMapper.toCommand(folio, request));
        return ResponseEntity.ok(generalInfoRestMapper.toResponse(updated));
    }
}
