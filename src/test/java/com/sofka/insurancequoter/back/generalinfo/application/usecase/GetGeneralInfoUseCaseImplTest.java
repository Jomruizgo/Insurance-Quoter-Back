package com.sofka.insurancequoter.back.generalinfo.application.usecase;

import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetGeneralInfoUseCaseImplTest {

    @Mock
    private GeneralInfoRepository generalInfoRepository;

    @InjectMocks
    private GetGeneralInfoUseCaseImpl useCase;

    private static final Instant NOW = Instant.parse("2026-04-23T19:55:12Z");

    private GeneralInfo buildGeneralInfo() {
        return new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                NOW,
                1L
        );
    }

    @Test
    void getGeneralInfo_whenFolioExists_returnsGeneralInfo() {
        // GIVEN
        when(generalInfoRepository.findByFolioNumber("FOL-2026-00001"))
                .thenReturn(Optional.of(buildGeneralInfo()));

        // WHEN
        GeneralInfo result = useCase.getGeneralInfo("FOL-2026-00001");

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(result.quoteStatus()).isEqualTo("CREATED");
        assertThat(result.insuredData().name()).isEqualTo("Empresa SA");
        assertThat(result.insuredData().rfc()).isEqualTo("EMP123456ABC");
        assertThat(result.underwritingInfo().subscriberId()).isEqualTo("SUB-001");
        assertThat(result.underwritingInfo().riskClassification()).isEqualTo(RiskClassification.STANDARD);
        assertThat(result.version()).isEqualTo(1L);
    }

    @Test
    void getGeneralInfo_whenFolioNotFound_throwsFolioNotFoundException() {
        // GIVEN
        when(generalInfoRepository.findByFolioNumber("UNKNOWN"))
                .thenReturn(Optional.empty());

        // WHEN / THEN
        assertThatThrownBy(() -> useCase.getGeneralInfo("UNKNOWN"))
                .isInstanceOf(FolioNotFoundException.class);
    }
}
