package com.sofka.insurancequoter.back.folio.domain.port.out;

import com.sofka.insurancequoter.back.folio.domain.model.FolioRaw;

import java.util.List;

// Output port: retrieves raw folio data from persistence.
// Does NOT resolve agentName or completionPct — those are use-case concerns.
public interface FolioListQuery {
    List<FolioRaw> findAll();
}
