package com.sofka.insurancequoter.back.folio.domain.port.in;

import com.sofka.insurancequoter.back.folio.domain.model.FolioSummary;

import java.util.List;

// Input port: returns all folio summaries enriched with agent name and completion percentage.
public interface ListFoliosUseCase {
    List<FolioSummary> listFolios();
}
