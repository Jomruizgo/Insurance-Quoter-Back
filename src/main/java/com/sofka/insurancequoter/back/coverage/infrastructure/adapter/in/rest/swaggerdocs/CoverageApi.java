package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.request.SaveCoverageOptionsRequest;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.in.rest.dto.response.CoverageOptionsListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Swagger/OpenAPI interface for coverage options endpoints — keeps controllers clean
@Tag(name = "Coverage Options", description = "Gestión de opciones de cobertura de cotización")
@RequestMapping("/v1/quotes/{folio}/coverage-options")
public interface CoverageApi {

    @Operation(summary = "Consultar opciones de cobertura")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opciones de cobertura retornadas"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado")
    })
    @GetMapping
    ResponseEntity<CoverageOptionsListResponse> getCoverageOptions(@PathVariable String folio);

    @Operation(summary = "Configurar opciones de cobertura")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Opciones de cobertura guardadas"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de versión optimista"),
            @ApiResponse(responseCode = "422", description = "Error de validación")
    })
    @PutMapping
    ResponseEntity<CoverageOptionsListResponse> saveCoverageOptions(
            @PathVariable String folio,
            @Valid @RequestBody SaveCoverageOptionsRequest request);
}
