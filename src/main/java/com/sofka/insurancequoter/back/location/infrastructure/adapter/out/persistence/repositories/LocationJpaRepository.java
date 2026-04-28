package com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories;

import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationJpaRepository extends JpaRepository<LocationJpa, Long> {

    List<LocationJpa> findByQuoteIdAndActiveTrue(Long quoteId);

    List<LocationJpa> findByQuoteId(Long quoteId);

    List<LocationJpa> findByQuoteIdAndIndexIn(Long quoteId, List<Integer> indices);

    Optional<LocationJpa> findByQuoteIdAndIndex(Long quoteId, Integer index);

    boolean existsByQuoteIdAndIndex(Long quoteId, Integer index);

    int countByQuoteId(Long quoteId);

    @Modifying
    @Query("DELETE FROM LocationJpa l WHERE l.quoteId = :quoteId")
    void deleteByQuoteId(@Param("quoteId") Long quoteId);
}
