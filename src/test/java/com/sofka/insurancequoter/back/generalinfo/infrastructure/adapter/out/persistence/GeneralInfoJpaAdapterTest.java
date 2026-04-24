package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.out.persistence.adapter.GeneralInfoJpaAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneralInfoJpaAdapterTest {

    @Mock
    private QuoteJpaRepository quoteJpaRepository;

    @InjectMocks
    private GeneralInfoJpaAdapter adapter;

    private static final Instant NOW = Instant.parse("2026-04-23T19:55:12Z");

    private QuoteJpa buildJpaEntity() {
        return QuoteJpa.builder()
                .folioNumber("FOL-2026-00001")
                .quoteStatus("CREATED")
                .subscriberId("SUB-001")
                .agentCode("AGT-123")
                .insuredName("Empresa SA")
                .insuredRfc("EMP123456ABC")
                .insuredEmail("empresa@test.com")
                .insuredPhone("5551234567")
                .riskClassification("STANDARD")
                .businessType("COMMERCIAL")
                .version(1L)
                .updatedAt(NOW)
                .build();
    }

    @Test
    void findByFolioNumber_whenExists_returnsGeneralInfo() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber("FOL-2026-00001"))
                .thenReturn(Optional.of(buildJpaEntity()));

        // WHEN
        Optional<GeneralInfo> result = adapter.findByFolioNumber("FOL-2026-00001");

        // THEN
        assertThat(result).isPresent();
        assertThat(result.get().folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(result.get().insuredData().name()).isEqualTo("Empresa SA");
        assertThat(result.get().underwritingInfo().riskClassification()).isEqualTo(RiskClassification.STANDARD);
        assertThat(result.get().version()).isEqualTo(1L);
    }

    @Test
    void findByFolioNumber_whenNotFound_returnsEmpty() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber("UNKNOWN"))
                .thenReturn(Optional.empty());

        // WHEN
        Optional<GeneralInfo> result = adapter.findByFolioNumber("UNKNOWN");

        // THEN
        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsAndReturnsMappedDomain() {
        // GIVEN
        GeneralInfo toSave = new GeneralInfo(
                "FOL-2026-00001",
                "CREATED",
                new InsuredData("Empresa SA", "EMP123456ABC", "empresa@test.com", "5551234567"),
                new UnderwritingInfo("SUB-001", "AGT-123", RiskClassification.STANDARD, BusinessType.COMMERCIAL),
                NOW,
                1L
        );
        when(quoteJpaRepository.findByFolioNumber("FOL-2026-00001"))
                .thenReturn(Optional.of(buildJpaEntity()));
        when(quoteJpaRepository.save(any())).thenReturn(buildJpaEntity());

        // WHEN
        GeneralInfo result = adapter.save(toSave);

        // THEN
        assertThat(result.folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(result.insuredData().rfc()).isEqualTo("EMP123456ABC");
        assertThat(result.underwritingInfo().agentCode()).isEqualTo("AGT-123");
    }
}
