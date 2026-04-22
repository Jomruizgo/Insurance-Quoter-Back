package com.sofka.insurancequoter.back.coverage.domain.port.out;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;

import java.util.List;

// Output port: persistence operations for coverage options
public interface CoverageOptionRepository {

    List<CoverageOption> findByFolioNumber(String folioNumber);

    List<CoverageOption> replaceAll(String folioNumber, List<CoverageOption> options);
}
