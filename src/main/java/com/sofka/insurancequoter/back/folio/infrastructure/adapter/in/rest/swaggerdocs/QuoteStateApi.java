package com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.swaggerdocs;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.in.rest.dto.QuoteStateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Quote State", description = "Estado y progreso de completitud de la cotización")
public interface QuoteStateApi {

    @Operation(summary = "Consultar estado de la cotización",
               description = "Retorna el estado actual, el porcentaje de completitud y el estado de cada sección del folio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Estado retornado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Folio no encontrado")
    })
    ResponseEntity<QuoteStateResponse> getState(@PathVariable String folioNumber);
}
