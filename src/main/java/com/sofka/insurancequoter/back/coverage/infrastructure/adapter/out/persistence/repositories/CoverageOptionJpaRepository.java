package com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.entities.CoverageOptionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// Spring Data repository for coverage_options table
public interface CoverageOptionJpaRepository extends JpaRepository<CoverageOptionJpa, Long> {

    List<CoverageOptionJpa> findAllByQuote_Id(Long quoteId);

    // Bulk DELETE to guarantee SQL execution order before saveAll within the same TX
    @Modifying
    @Query("DELETE FROM CoverageOptionJpa c WHERE c.quote.id = :quoteId")
    void deleteAllByQuote_Id(@Param("quoteId") Long quoteId);
}
