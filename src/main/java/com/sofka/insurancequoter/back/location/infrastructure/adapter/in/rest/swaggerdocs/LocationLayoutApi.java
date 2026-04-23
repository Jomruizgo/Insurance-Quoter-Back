package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.GetLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Location Layout", description = "Manages the layout configuration of locations per quote")
public interface LocationLayoutApi {

    @Operation(summary = "Get layout configuration for a quote")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layout returned successfully"),
            @ApiResponse(responseCode = "404", description = "Folio not found")
    })
    ResponseEntity<GetLayoutResponse> getLayout(
            @Parameter(description = "Folio number", required = true)
            @PathVariable String folio
    );

    @Operation(summary = "Save or update layout configuration for a quote")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Layout saved successfully"),
            @ApiResponse(responseCode = "404", description = "Folio not found"),
            @ApiResponse(responseCode = "409", description = "Optimistic lock conflict"),
            @ApiResponse(responseCode = "422", description = "Validation error")
    })
    ResponseEntity<SaveLayoutResponse> saveLayout(
            @Parameter(description = "Folio number", required = true)
            @PathVariable String folio,
            @Valid @RequestBody SaveLayoutRequest request
    );
}
