package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.CalculationResultJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Spring Data repository for the calculation_results table
public interface CalculationResultJpaRepository extends JpaRepository<CalculationResultJpa, Long> {

    Optional<CalculationResultJpa> findByQuoteId(Long quoteId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CalculationResultJpa c WHERE c.quote.id = :quoteId")
    void deleteByQuoteId(@Param("quoteId") Long quoteId);
}
