package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.location.application.usecase.GetLayoutResult;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutResult;
import com.sofka.insurancequoter.back.location.domain.port.in.GetLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.domain.port.in.SaveLocationLayoutUseCase;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.GetLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper.LocationLayoutRestMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.swaggerdocs.LocationLayoutApi;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// REST adapter for the location layout bounded context
@RestController
@RequestMapping("/v1/quotes/{folio}/locations/layout")
public class LocationLayoutController implements LocationLayoutApi {

    private final GetLocationLayoutUseCase getLayoutUseCase;
    private final SaveLocationLayoutUseCase saveLayoutUseCase;
    private final LocationLayoutRestMapper mapper;

    public LocationLayoutController(GetLocationLayoutUseCase getLayoutUseCase,
                                    SaveLocationLayoutUseCase saveLayoutUseCase,
                                    LocationLayoutRestMapper mapper) {
        this.getLayoutUseCase = getLayoutUseCase;
        this.saveLayoutUseCase = saveLayoutUseCase;
        this.mapper = mapper;
    }

    @Override
    @GetMapping
    public ResponseEntity<GetLayoutResponse> getLayout(@PathVariable String folio) {
        GetLayoutResult result = getLayoutUseCase.getLayout(folio);
        return ResponseEntity.ok(mapper.toGetResponse(result));
    }

    @Override
    @PutMapping
    public ResponseEntity<SaveLayoutResponse> saveLayout(
            @PathVariable String folio,
            @Valid @RequestBody SaveLayoutRequest request) {
        SaveLayoutResult result = saveLayoutUseCase.saveLayout(mapper.toCommand(folio, request));
        return ResponseEntity.ok(mapper.toSaveResponse(result));
    }
}
