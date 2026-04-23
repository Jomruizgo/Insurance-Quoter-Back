package com.sofka.insurancequoter.back.generalinfo.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.generalinfo.domain.model.BusinessType;
import com.sofka.insurancequoter.back.generalinfo.domain.model.GeneralInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.model.InsuredData;
import com.sofka.insurancequoter.back.generalinfo.domain.model.RiskClassification;
import com.sofka.insurancequoter.back.generalinfo.domain.model.UnderwritingInfo;
import com.sofka.insurancequoter.back.generalinfo.domain.port.out.GeneralInfoRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

// JPA adapter: implements GeneralInfoRepository output port using QuoteJpa from the folio context.
// This avoids dual-entity mapping of the quotes table.
@Component
@RequiredArgsConstructor
public class GeneralInfoJpaAdapter implements GeneralInfoRepository {

    private final QuoteJpaRepository quoteJpaRepository;

    @Override
    public Optional<GeneralInfo> findByFolioNumber(String folioNumber) {
        return quoteJpaRepository.findByFolioNumber(folioNumber)
                .map(this::toDomain);
    }

    @Override
    public GeneralInfo save(GeneralInfo generalInfo) {
        QuoteJpa existing = quoteJpaRepository.findByFolioNumber(generalInfo.folioNumber())
                .orElseThrow(() -> new FolioNotFoundException(generalInfo.folioNumber()));

        existing.setInsuredName(generalInfo.insuredData().name());
        existing.setInsuredRfc(generalInfo.insuredData().rfc());
        existing.setInsuredEmail(generalInfo.insuredData().email());
        existing.setInsuredPhone(generalInfo.insuredData().phone());
        existing.setRiskClassification(generalInfo.underwritingInfo().riskClassification().name());
        existing.setBusinessType(generalInfo.underwritingInfo().businessType().name());

        QuoteJpa saved = quoteJpaRepository.save(existing);
        return toDomain(saved);
    }

    private GeneralInfo toDomain(QuoteJpa jpa) {
        return new GeneralInfo(
                jpa.getFolioNumber(),
                jpa.getQuoteStatus(),
                new InsuredData(
                        jpa.getInsuredName(),
                        jpa.getInsuredRfc(),
                        jpa.getInsuredEmail(),
                        jpa.getInsuredPhone()
                ),
                new UnderwritingInfo(
                        jpa.getSubscriberId(),
                        jpa.getAgentCode(),
                        jpa.getRiskClassification() != null
                                ? RiskClassification.valueOf(jpa.getRiskClassification())
                                : null,
                        jpa.getBusinessType() != null
                                ? BusinessType.valueOf(jpa.getBusinessType())
                                : null
                ),
                jpa.getUpdatedAt(),
                jpa.getVersion()
        );
    }
}
