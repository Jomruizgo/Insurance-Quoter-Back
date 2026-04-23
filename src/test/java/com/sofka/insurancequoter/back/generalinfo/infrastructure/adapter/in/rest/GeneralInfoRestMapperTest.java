package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest;

import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.dto.GeneralInfoResponse;
import com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.in.rest.mapper.GeneralInfoRestMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class GeneralInfoRestMapperTest {

    private final GeneralInfoRestMapper mapper = new GeneralInfoRestMapper();

    private static final Instant NOW = Instant.parse("2026-04-23T19:55:12Z");

    private GeneralInfo buildDomain() {
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
    void toResponse_mapsAllFieldsCorrectly() {
        // GIVEN
        GeneralInfo domain = buildDomain();

        // WHEN
        GeneralInfoResponse response = mapper.toResponse(domain);

        // THEN
        assertThat(response.folioNumber()).isEqualTo("FOL-2026-00001");
        assertThat(response.quoteStatus()).isEqualTo("CREATED");
        assertThat(response.insuredData().name()).isEqualTo("Empresa SA");
        assertThat(response.insuredData().rfc()).isEqualTo("EMP123456ABC");
        assertThat(response.insuredData().email()).isEqualTo("empresa@test.com");
        assertThat(response.insuredData().phone()).isEqualTo("5551234567");
        assertThat(response.underwritingData().subscriberId()).isEqualTo("SUB-001");
        assertThat(response.underwritingData().agentCode()).isEqualTo("AGT-123");
        assertThat(response.underwritingData().riskClassification()).isEqualTo("STANDARD");
        assertThat(response.underwritingData().businessType()).isEqualTo("COMMERCIAL");
        assertThat(response.updatedAt()).isEqualTo(NOW);
        assertThat(response.version()).isEqualTo(1L);
    }
}
