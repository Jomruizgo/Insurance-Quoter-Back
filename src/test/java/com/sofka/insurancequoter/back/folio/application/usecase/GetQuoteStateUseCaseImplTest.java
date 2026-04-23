package com.sofka.insurancequoter.back.folio.application.usecase;

import com.sofka.insurancequoter.back.folio.domain.model.*;
import com.sofka.insurancequoter.back.folio.domain.port.out.LocationStateReader;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteStateQuery;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetQuoteStateUseCaseImplTest {

    @Mock
    private QuoteStateQuery quoteStateQuery;

    @Mock
    private LocationStateReader locationStateReader;

    @InjectMocks
    private GetQuoteStateUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-21T10:00:00Z");

    // hasGeneralInfo = false (insured_name null)
    private QuoteSnapshot snapshot(String status, Integer numLocations, String locationType) {
        return new QuoteSnapshot("FOL-001", status, numLocations, locationType, 3L, NOW, false);
    }

    // hasGeneralInfo = true (insured_name present)
    private QuoteSnapshot snapshotWithGeneralInfo(String status, Integer numLocations, String locationType) {
        return new QuoteSnapshot("FOL-001", status, numLocations, locationType, 3L, NOW, true);
    }

    @Test
    void getState_withLayoutCompleteAndOneIncompleteLocation_returnsPartialProgress() {
        // GIVEN — layout COMPLETE, one incomplete location → 1/5 = 20%
        when(quoteStateQuery.findByFolioNumber("FOL-001"))
                .thenReturn(snapshot("IN_PROGRESS", 2, "MULTIPLE"));
        when(locationStateReader.readByFolioNumber("FOL-001"))
                .thenReturn(new LocationStateSummary(2, 1, 1)); // 1 complete, 1 incomplete

        // WHEN
        QuoteState state = useCase.getState("FOL-001");

        // THEN
        assertThat(state.folioNumber()).isEqualTo("FOL-001");
        assertThat(state.quoteStatus()).isEqualTo("IN_PROGRESS");
        assertThat(state.sections().layout()).isEqualTo(SectionStatus.COMPLETE);
        assertThat(state.sections().locations()).isEqualTo(SectionStatus.INCOMPLETE);
        assertThat(state.sections().generalInfo()).isEqualTo(SectionStatus.PENDING);
        assertThat(state.sections().coverageOptions()).isEqualTo(SectionStatus.PENDING);
        assertThat(state.sections().calculation()).isEqualTo(SectionStatus.PENDING);
        assertThat(state.completionPercentage()).isEqualTo(20); // 1 COMPLETE (layout) / 5 * 100
    }

    @Test
    void getState_withFreshFolio_returnsZeroPercentage() {
        // GIVEN — no layout, no locations
        when(quoteStateQuery.findByFolioNumber("FOL-001"))
                .thenReturn(snapshot("CREATED", null, null));
        when(locationStateReader.readByFolioNumber("FOL-001"))
                .thenReturn(new LocationStateSummary(0, 0, 0));

        // WHEN
        QuoteState state = useCase.getState("FOL-001");

        // THEN
        assertThat(state.completionPercentage()).isEqualTo(0);
        assertThat(state.sections().layout()).isEqualTo(SectionStatus.PENDING);
        assertThat(state.sections().locations()).isEqualTo(SectionStatus.PENDING);
    }

    @Test
    void getState_withCalculatedFolio_returns100Percent() {
        // GIVEN — quoteStatus = CALCULATED → forced 100%
        when(quoteStateQuery.findByFolioNumber("FOL-001"))
                .thenReturn(snapshot("CALCULATED", 2, "MULTIPLE"));
        when(locationStateReader.readByFolioNumber("FOL-001"))
                .thenReturn(new LocationStateSummary(2, 2, 0));

        // WHEN
        QuoteState state = useCase.getState("FOL-001");

        // THEN
        assertThat(state.completionPercentage()).isEqualTo(100);
        assertThat(state.sections().calculation()).isEqualTo(SectionStatus.COMPLETE);
    }

    @Test
    void getState_folioNotFound_throwsFolioNotFoundException() {
        // GIVEN
        when(quoteStateQuery.findByFolioNumber("UNKNOWN"))
                .thenThrow(new FolioNotFoundException("UNKNOWN"));

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getState("UNKNOWN"))
                .isInstanceOf(FolioNotFoundException.class);
    }

    @Test
    void getState_whenGeneralInfoFieldsNull_returnsGeneralInfoPending() {
        // GIVEN — insured_name is null → hasGeneralInfo = false
        when(quoteStateQuery.findByFolioNumber("FOL-001"))
                .thenReturn(snapshot("IN_PROGRESS", 3, "MULTIPLE"));
        when(locationStateReader.readByFolioNumber("FOL-001"))
                .thenReturn(new LocationStateSummary(3, 3, 0));

        // WHEN
        QuoteState state = useCase.getState("FOL-001");

        // THEN
        assertThat(state.sections().generalInfo()).isEqualTo(SectionStatus.PENDING);
        assertThat(state.sections().coverageOptions()).isEqualTo(SectionStatus.PENDING);
    }

    @Test
    void getState_whenGeneralInfoSaved_returnsGeneralInfoComplete() {
        // GIVEN — insured_name is present → hasGeneralInfo = true
        when(quoteStateQuery.findByFolioNumber("FOL-001"))
                .thenReturn(snapshotWithGeneralInfo("IN_PROGRESS", 3, "MULTIPLE"));
        when(locationStateReader.readByFolioNumber("FOL-001"))
                .thenReturn(new LocationStateSummary(0, 0, 0));

        // WHEN
        QuoteState state = useCase.getState("FOL-001");

        // THEN
        assertThat(state.sections().generalInfo()).isEqualTo(SectionStatus.COMPLETE);
    }
}
