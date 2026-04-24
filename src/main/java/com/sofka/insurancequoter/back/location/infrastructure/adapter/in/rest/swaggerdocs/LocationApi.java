package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request.PatchLocationRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request.ReplaceLocationsRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationPatchWrapperResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsListResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsListResponseWithTimestamp;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.LocationsSummaryWrapperResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Locations", description = "Manage locations within a quote")
public interface LocationApi {

    @Operation(summary = "Get all locations for a quote")
    ResponseEntity<LocationsListResponse> getLocations(@PathVariable String folio);

    @Operation(summary = "Replace all locations for a quote")
    ResponseEntity<LocationsListResponseWithTimestamp> replaceLocations(@PathVariable String folio,
                                                                        @RequestBody @Valid ReplaceLocationsRequest request);

    @Operation(summary = "Get locations validation summary")
    ResponseEntity<LocationsSummaryWrapperResponse> getLocationsSummary(@PathVariable String folio);

    @Operation(summary = "Patch a single location")
    ResponseEntity<LocationPatchWrapperResponse> patchLocation(@PathVariable String folio,
                                                               @PathVariable int index,
                                                               @RequestBody PatchLocationRequest request);
}
