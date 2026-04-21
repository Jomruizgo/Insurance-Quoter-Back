package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.mappers;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;

// Converts between the domain model (Quote) and the JPA entity (QuoteJpa)
public class QuotePersistenceMapper {

    public QuoteJpa toJpa(Quote quote) {
        QuoteJpa jpa = new QuoteJpa();
        jpa.setFolioNumber(quote.folioNumber());
        jpa.setQuoteStatus(quote.quoteStatus().name());
        jpa.setSubscriberId(quote.subscriberId());
        jpa.setAgentCode(quote.agentCode());
        // id, version, createdAt, updatedAt are managed by JPA/Hibernate
        return jpa;
    }

    public Quote toDomain(QuoteJpa jpa) {
        return new Quote(
                jpa.getFolioNumber(),
                QuoteStatus.valueOf(jpa.getQuoteStatus()),
                jpa.getSubscriberId(),
                jpa.getAgentCode(),
                jpa.getVersion(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }
}
