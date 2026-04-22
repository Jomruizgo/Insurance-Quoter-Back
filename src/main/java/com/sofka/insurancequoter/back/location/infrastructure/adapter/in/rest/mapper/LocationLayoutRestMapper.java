package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.location.application.usecase.GetLayoutResult;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutCommand;
import com.sofka.insurancequoter.back.location.application.usecase.SaveLayoutResult;
import com.sofka.insurancequoter.back.location.domain.model.LayoutConfiguration;
import com.sofka.insurancequoter.back.location.domain.model.LocationType;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.GetLayoutResponse;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.LayoutConfigurationDto;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutRequest;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.SaveLayoutResponse;
import org.springframework.stereotype.Component;

// Maps between REST DTOs and application layer command/result objects
@Component
public class LocationLayoutRestMapper {

    public GetLayoutResponse toGetResponse(GetLayoutResult result) {
        LayoutConfiguration layout = result.layoutConfiguration();
        String locationTypeName = layout.locationType() != null
                ? layout.locationType().name()
                : null;
        LayoutConfigurationDto dto = new LayoutConfigurationDto(
                layout.numberOfLocations(),
                locationTypeName
        );
        return new GetLayoutResponse(result.folioNumber(), dto, result.version());
    }

    public SaveLayoutCommand toCommand(String folioNumber, SaveLayoutRequest request) {
        LayoutConfigurationDto dto = request.layoutConfiguration();
        LocationType locationType = dto.locationType() != null
                ? LocationType.valueOf(dto.locationType())
                : null;
        LayoutConfiguration layout = new LayoutConfiguration(dto.numberOfLocations(), locationType);
        return new SaveLayoutCommand(folioNumber, layout, request.version());
    }

    public SaveLayoutResponse toSaveResponse(SaveLayoutResult result) {
        LayoutConfiguration layout = result.layoutConfiguration();
        String locationTypeName = layout.locationType() != null
                ? layout.locationType().name()
                : null;
        LayoutConfigurationDto dto = new LayoutConfigurationDto(
                layout.numberOfLocations(),
                locationTypeName
        );
        return new SaveLayoutResponse(result.folioNumber(), dto, result.updatedAt(), result.version());
    }
}
