package com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.mapper;

import com.sofka.insurancequoter.back.location.domain.model.*;
import com.sofka.insurancequoter.back.location.domain.port.in.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.request.*;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.in.rest.dto.response.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

// Maps between REST DTOs and domain port types for the location management feature
@Component
public class LocationRestMapper {

    // --- Request → Command ---

    public ReplaceLocationsUseCase.ReplaceLocationsCommand toReplaceCommand(
            String folio, ReplaceLocationsRequest request) {
        List<ReplaceLocationsUseCase.LocationData> locations = request.locations().stream()
                .map(this::toLocationData)
                .toList();
        return new ReplaceLocationsUseCase.ReplaceLocationsCommand(folio, locations, request.version());
    }

    private ReplaceLocationsUseCase.LocationData toLocationData(LocationItemRequest req) {
        BusinessLine bl = req.businessLine() != null
                ? new BusinessLine(req.businessLine().code(), req.businessLine().fireKey(), req.businessLine().description())
                : null;
        List<Guarantee> guarantees = req.guarantees() != null
                ? req.guarantees().stream().map(g -> new Guarantee(g.code(), g.insuredValue())).toList()
                : List.of();
        return new ReplaceLocationsUseCase.LocationData(
                req.index() != null ? req.index() : 0,
                req.locationName(), req.address(), req.zipCode(),
                req.constructionType(), req.level(), req.constructionYear(),
                bl, guarantees);
    }

    public PatchLocationUseCase.PatchLocationCommand toPatchCommand(
            String folio, int index, PatchLocationRequest request) {
        Optional<BusinessLine> bl = request.businessLine().map(b ->
                new BusinessLine(b.code(), b.fireKey(), b.description()));
        Optional<List<Guarantee>> guarantees = request.guarantees().map(list ->
                list.stream().map(g -> new Guarantee(g.code(), g.insuredValue())).toList());

        return new PatchLocationUseCase.PatchLocationCommand(
                folio, index, request.version(),
                request.locationName(), request.address(), request.zipCode(),
                request.constructionType(), request.level(), request.constructionYear(),
                bl, guarantees);
    }

    // --- Domain → Response ---

    public LocationsListResponse toLocationsListResponse(GetLocationsUseCase.LocationsResult result) {
        List<LocationDetailResponse> details = result.locations().stream()
                .map(this::toDetail)
                .toList();
        return new LocationsListResponse(result.folioNumber(), details, result.version());
    }

    public LocationsListResponseWithTimestamp toLocationsListResponseWithTimestamp(
            ReplaceLocationsUseCase.ReplaceLocationsResult result) {
        List<LocationDetailResponse> details = result.locations().stream()
                .map(this::toDetail)
                .toList();
        return new LocationsListResponseWithTimestamp(result.folioNumber(), details, result.updatedAt(), result.version());
    }

    public LocationPatchWrapperResponse toPatchWrapperResponse(PatchLocationUseCase.PatchLocationResult result) {
        return new LocationPatchWrapperResponse(
                result.folioNumber(),
                toDetail(result.location()),
                result.updatedAt(),
                result.version());
    }

    public LocationsSummaryWrapperResponse toSummaryWrapperResponse(
            GetLocationsSummaryUseCase.SummaryResult result) {
        List<LocationSummaryItemResponse> items = result.locations().stream()
                .map(s -> new LocationSummaryItemResponse(
                        s.index(), s.locationName(),
                        s.validationStatus() != null ? s.validationStatus().name() : null,
                        s.blockingAlerts().stream()
                                .map(a -> new BlockingAlertResponse(a.code(), a.message()))
                                .toList()))
                .toList();
        return new LocationsSummaryWrapperResponse(
                result.folioNumber(),
                result.totalLocations(),
                result.completeLocations(),
                result.incompleteLocations(),
                items);
    }

    private LocationDetailResponse toDetail(Location loc) {
        BusinessLineResponse bl = loc.businessLine() != null
                ? new BusinessLineResponse(
                        loc.businessLine().code(),
                        loc.businessLine().fireKey(),
                        loc.businessLine().description())
                : null;
        List<GuaranteeResponse> guarantees = loc.guarantees() != null
                ? loc.guarantees().stream()
                        .map(g -> new GuaranteeResponse(g.code(), g.insuredValue()))
                        .toList()
                : List.of();
        List<BlockingAlertResponse> alerts = loc.blockingAlerts() != null
                ? loc.blockingAlerts().stream()
                        .map(a -> new BlockingAlertResponse(a.code(), a.message()))
                        .toList()
                : List.of();
        return new LocationDetailResponse(
                loc.index(), loc.locationName(), loc.address(), loc.zipCode(),
                loc.state(), loc.municipality(), loc.neighborhood(), loc.city(),
                loc.constructionType(), loc.level(), loc.constructionYear(),
                bl, guarantees, loc.catastrophicZone(),
                loc.validationStatus() != null ? loc.validationStatus().name() : null,
                alerts);
    }
}
