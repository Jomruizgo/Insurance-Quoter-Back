package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.mappers;

import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities.CoverageOptionJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;

import java.util.List;

// Bidirectional mapper between CoverageOption (domain) and CoverageOptionJpa (persistence)
public class CoverageOptionPersistenceMapper {

    public CoverageOption toDomain(CoverageOptionJpa jpa) {
        return new CoverageOption(
                jpa.getCode(),
                jpa.getDescription(),
                jpa.isSelected(),
                jpa.getDeductiblePercentage(),
                jpa.getCoinsurancePercentage()
        );
    }

    public CoverageOptionJpa toJpa(CoverageOption domain, QuoteJpa quote) {
        return CoverageOptionJpa.builder()
                .quote(quote)
                .code(domain.code())
                .description(domain.description())
                .selected(domain.selected())
                .deductiblePercentage(domain.deductiblePercentage())
                .coinsurancePercentage(domain.coinsurancePercentage())
                .build();
    }

    public List<CoverageOption> toDomainList(List<CoverageOptionJpa> jpaList) {
        return jpaList.stream().map(this::toDomain).toList();
    }

    public List<CoverageOptionJpa> toJpaList(List<CoverageOption> domainList, QuoteJpa quote) {
        return domainList.stream().map(d -> toJpa(d, quote)).toList();
    }
}
