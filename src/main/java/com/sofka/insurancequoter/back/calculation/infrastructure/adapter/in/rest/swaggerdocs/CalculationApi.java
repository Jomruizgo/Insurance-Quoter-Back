package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.request.CalculatePremiumRequest;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.in.rest.dto.response.CalculationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

// Swagger/OpenAPI interface for the Calculation bounded context — controllers implement this
@Tag(name = "Calculation", description = "Cálculo de prima neta y comercial")
@RequestMapping("/v1/quotes")
public interface CalculationApi {

    @Operation(summary = "Calcular prima de cotización",
               description = "Ejecuta el cálculo de prima neta y comercial para todas las ubicaciones calculables")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cálculo exitoso"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de versión (optimistic lock)"),
            @ApiResponse(responseCode = "422", description = "Sin ubicaciones calculables o versión nula"),
            @ApiResponse(responseCode = "502", description = "Core service no disponible")
    })
    @PostMapping("/{folio}/calculate")
    ResponseEntity<CalculationResponse> calculate(
            @PathVariable String folio,
            @Valid @RequestBody CalculatePremiumRequest request
    );
}
