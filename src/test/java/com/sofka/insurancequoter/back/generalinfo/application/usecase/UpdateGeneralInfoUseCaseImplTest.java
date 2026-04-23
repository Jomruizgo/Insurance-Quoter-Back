package com.sofka.insurancequoter.back.generalinfo.application.usecase;

import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.application.usecase.VersionConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateGeneralInfoUseCaseImplTest {

    @Mock
    private GeneralInfoRepository generalInfoRepository;

    @InjectMocks
    private UpdateGeneralInfoUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-23T19:55:12Z");

    private GeneralInfo storedGeneralInfo(Long version) {
        return new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Old Name", "OLD123456ABC", "old@test.com", "5550000000"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                NOW,
                version
        );
    }

    private UpdateGeneralInfoCommand buildCommand(Long version) {
        return new UpdateGeneralInfoCommand(
                "FOL-2026-00001",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                version
        );
    }

    @Test
    void updateGeneralInfo_whenVersionMatches_returnsUpdatedGeneralInfo() {
        // GIVEN
        when(generalInfoRepository.findByFolioNumber("FOL-2026-00001"))
                .thenReturn(Optional.of(storedGeneralInfo(1L)));

        GeneralInfo savedResult = new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                NOW,
                2L
        );
        when(generalInfoRepository.save(any())).thenReturn(savedResult);

        // WHEN
        GeneralInfo result = useCase.updateGeneralInfo(buildCommand(1L));

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(result.insuredData().name()).isEqualTo("Empresa SA");
        assertThat(result.insuredData().rfc()).isEqualTo("EMP123456ABC");
        assertThat(result.underwritingInfo().riskClassification()).isEqualTo(RiskClassification.STANDARD);
        assertThat(result.version()).isEqualTo(2L);
    }

    @Test
    void updateGeneralInfo_whenFolioNotFound_throwsFolioNotFoundException() {
        // GIVEN
        when(generalInfoRepository.findByFolioNumber("UNKNOWN"))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.updateGeneralInfo(
                new UpdateGeneralInfoCommand(
                        "UNKNOWN",
                        new InsuredData("N", "X", "x@x.com", "1"),
                        new UnderwritingInfo("S", "A", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                        0L
                )))
                .isInstanceOf(FolioNotFoundException.class);
    }

    @Test
    void updateGeneralInfo_whenVersionConflict_throwsVersionConflictException() {
        // GIVEN - stored version is 2, client sends 1
        when(generalInfoRepository.findByFolioNumber("FOL-2026-00001"))
                .thenReturn(Optional.of(storedGeneralInfo(2L)));

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.updateGeneralInfo(buildCommand(1L)))
                .isInstanceOf(VersionConflictException.class);
    }
}
