package com.sofka.insurancequoter.back.location.domain.port.in;

import com.sofka.insurancequoter.back.location.domain.model.BusinessLine;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.domain.model.Location;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

// Input port for partially updating a single location within a quote
public interface PatchLocationUseCase {

    PatchLocationResult patchLocation(PatchLocationCommand command);

    record PatchLocationCommand(
            String folioNumber,
            int index,
            Long version,
            Optional<String> locationName,
            Optional<String> address,
            Optional<String> zipCode,
            Optional<String> constructionType,
            Optional<Integer> level,
            Optional<Integer> constructionYear,
            Optional<BusinessLine> businessLine,
            Optional<List<Guarantee>> guarantees
    ) {}

    record PatchLocationResult(
            String folioNumber,
            Location location,
            Instant updatedAt,
            Long version
    ) {}
}
