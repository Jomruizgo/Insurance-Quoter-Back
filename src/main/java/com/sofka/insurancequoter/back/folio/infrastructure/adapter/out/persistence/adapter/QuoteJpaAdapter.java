package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.adapter;

import com.sofka.insurancequoter.back.folio.domain.model.Quote;
import com.sofka.insurancequoter.back.folio.domain.model.QuoteStatus;
import com.sofka.insurancequoter.back.folio.domain.port.out.QuoteRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.mappers.QuotePersistenceMapper;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Adapter: implements the QuoteRepository output port using Spring Data JPA
@Component
@RequiredArgsConstructor
public class QuoteJpaAdapter implements QuoteRepository {

    private final QuoteJpaRepository jpaRepository;
    private final QuotePersistenceMapper mapper;

    @Override
    public Optional<Quote> findActiveBySubscriberAndAgent(String subscriberId, String agentCode) {
        // Only CREATED status is considered "active" for idempotency purposes
        return jpaRepository
                .findBySubscriberIdAndAgentCodeAndQuoteStatus(
                        subscriberId,
                        agentCode,
                        QuoteStatus.CREATED.name())
                .map(mapper::toDomain);
    }

    @Override
    public Quote save(Quote quote) {
        QuoteJpa jpa = mapper.toJpa(quote);
        QuoteJpa saved = jpaRepository.save(jpa);
        return mapper.toDomain(saved);
    }
}
