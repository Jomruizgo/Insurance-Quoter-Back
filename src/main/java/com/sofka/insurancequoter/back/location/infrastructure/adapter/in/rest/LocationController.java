package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.location.domain.port.in.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request.PatchLocationRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request.ReplaceLocationsRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationPatchWrapperResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsListResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsListResponseWithTimestamp;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsSummaryWrapperResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper.LocationRestMapper;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.swaggerdocs.LocationApi;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// REST controller for location management operations on a quote
@RestController
@RequestMapping("/v1/quotes/{folio}/locations")
public class LocationController implements LocationApi {

    private final GetLocationsUseCase getLocationsUseCase;
    private final ReplaceLocationsUseCase replaceLocationsUseCase;
    private final PatchLocationUseCase patchLocationUseCase;
    private final GetLocationsSummaryUseCase getLocationsSummaryUseCase;
    private final LocationRestMapper mapper;

    public LocationController(GetLocationsUseCase getLocationsUseCase,
                              ReplaceLocationsUseCase replaceLocationsUseCase,
                              PatchLocationUseCase patchLocationUseCase,
                              GetLocationsSummaryUseCase getLocationsSummaryUseCase,
                              LocationRestMapper mapper) {
        this.getLocationsUseCase = getLocationsUseCase;
        this.replaceLocationsUseCase = replaceLocationsUseCase;
        this.patchLocationUseCase = patchLocationUseCase;
        this.getLocationsSummaryUseCase = getLocationsSummaryUseCase;
        this.mapper = mapper;
    }

    @Override
    @GetMapping
    public ResponseEntity<LocationsListResponse> getLocations(@PathVariable String folio) {
        var result = getLocationsUseCase.getLocations(folio);
        return ResponseEntity.ok(mapper.toLocationsListResponse(result));
    }

    @Override
    @PutMapping
    public ResponseEntity<LocationsListResponseWithTimestamp> replaceLocations(@PathVariable String folio,
                                                                               @RequestBody @Valid ReplaceLocationsRequest request) {
        var command = mapper.toReplaceCommand(folio, request);
        var result = replaceLocationsUseCase.replaceLocations(command);
        return ResponseEntity.ok(mapper.toLocationsListResponseWithTimestamp(result));
    }

    // NOTE: /summary must be declared BEFORE /{index} to avoid routing conflict
    @Override
    @GetMapping("/summary")
    public ResponseEntity<LocationsSummaryWrapperResponse> getLocationsSummary(@PathVariable String folio) {
        var result = getLocationsSummaryUseCase.getSummary(folio);
        return ResponseEntity.ok(mapper.toSummaryWrapperResponse(result));
    }

    @Override
    @PatchMapping("/{index}")
    public ResponseEntity<LocationPatchWrapperResponse> patchLocation(@PathVariable String folio,
                                                                      @PathVariable int index,
                                                                      @RequestBody PatchLocationRequest request) {
        var command = mapper.toPatchCommand(folio, index, request);
        var result = patchLocationUseCase.patchLocation(command);
        return ResponseEntity.ok(mapper.toPatchWrapperResponse(result));
    }
}
