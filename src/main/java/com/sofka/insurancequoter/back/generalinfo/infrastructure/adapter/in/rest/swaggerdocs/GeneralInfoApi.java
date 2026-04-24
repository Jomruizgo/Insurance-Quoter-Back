package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.GeneralInfoResponse;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.UpdateGeneralInfoRequest;
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

// Swagger annotations live here - GeneralInfoController stays clean
@Tag(name = "General Info", description = "Gestión de datos generales de cotización")
@RequestMapping("/v1/quotes/{folio}/general-info")
public interface GeneralInfoApi {

    @Operation(summary = "Obtener datos generales de la cotización")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos generales obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado")
    })
    @GetMapping
    ResponseEntity<GeneralInfoResponse> getGeneralInfo(@PathVariable("folio") String folio);

    @Operation(summary = "Actualizar datos generales de la cotización")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Datos generales actualizados exitosamente"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado"),
            @ApiResponse(responseCode = "409", description = "Conflicto de versión (optimistic lock)"),
            @ApiResponse(responseCode = "422", description = "Datos de entrada inválidos")
    })
    @PutMapping
    ResponseEntity<GeneralInfoResponse> updateGeneralInfo(
            @PathVariable("folio") String folio,
            @Valid @RequestBody UpdateGeneralInfoRequest request);
}
