package com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

// Spring Data derives the idempotency query from the method name
public interface QuoteJpaRepository extends JpaRepository<QuoteJpa, Long> {

    Optional<QuoteJpa> findBySubscriberIdAndAgentCodeAndQuoteStatus(
            String subscriberId,
            String agentCode,
            String quoteStatus
    );

    Optional<QuoteJpa> findByFolioNumber(String folioNumber);

    @Modifying
    @Query(value = "UPDATE quotes SET version = version + 1, updated_at = :now WHERE folio_number = :folioNumber", nativeQuery = true)
    void incrementVersionByFolioNumber(@Param("folioNumber") String folioNumber, @Param("now") Instant now);
}
