package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// Spring Data derives the idempotency query from the method name
public interface QuoteJpaRepository extends JpaRepository<QuoteJpa, Long> {

    Optional<QuoteJpa> findBySubscriberIdAndAgentCodeAndQuoteStatus(
            String subscriberId,
            String agentCode,
            String quoteStatus
    );
}
