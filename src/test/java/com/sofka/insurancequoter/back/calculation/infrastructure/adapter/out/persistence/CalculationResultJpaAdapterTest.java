package com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence;

import com.sofka.insurancequoter.back.calculation.application.usecase.dto.QuoteCalculationSnapshot;
import com.sofka.insurancequoter.back.calculation.domain.model.CalculationResult;
import com.sofka.insurancequoter.back.calculation.domain.model.CoverageBreakdown;
import com.sofka.insurancequoter.back.calculation.domain.model.PremiumByLocation;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.adapter.CalculationResultJpaAdapter;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.CalculationResultJpa;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.entities.PremiumByLocationJpa;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.mappers.CalculationPersistenceMapper;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.CalculationResultJpaRepository;
import com.sofka.insurancequoter.back.calculation.infrastructure.adapter.out.persistence.repositories.PremiumByLocationJpaRepository;
import com.sofka.insurancequoter.back.coverage.domain.model.CoverageOption;
import com.sofka.insurancequoter.back.coverage.infrastructure.adapter.out.persistence.repositories.CoverageOptionJpaRepository;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.entities.QuoteJpa;
import com.sofka.insurancequoter.back.folio.infrastructure.adapter.out.persistence.repositories.QuoteJpaRepository;
import com.sofka.insurancequoter.back.location.application.usecase.FolioNotFoundException;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlert;
import com.sofka.insurancequoter.back.location.domain.model.BlockingAlertCode;
import com.sofka.insurancequoter.back.location.domain.model.BusinessLine;
import com.sofka.insurancequoter.back.location.domain.model.Guarantee;
import com.sofka.insurancequoter.back.location.domain.model.Location;
import com.sofka.insurancequoter.back.location.domain.model.ValidationStatus;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.repositories.LocationJpaRepository;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.entities.LocationJpa;
import com.sofka.insurancequoter.back.location.infrastructure.adapter.out.persistence.mappers.LocationPersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for CalculationResultJpaAdapter — mocks all JPA repositories
@ExtendWith(MockitoExtension.class)
class CalculationResultJpaAdapterTest {

    @Mock private QuoteJpaRepository quoteJpaRepository;
    @Mock private LocationJpaRepository locationJpaRepository;
    @Mock private CoverageOptionJpaRepository coverageOptionJpaRepository;
    @Mock private CalculationResultJpaRepository calculationResultJpaRepository;
    @Mock private PremiumByLocationJpaRepository premiumByLocationJpaRepository;
    @Mock private CalculationPersistenceMapper calculationPersistenceMapper;
    @Mock private LocationPersistenceMapper locationPersistenceMapper;

    private CalculationResultJpaAdapter adapter;

    private static final String FOLIO = "FOL-2026-00042";
    private static final Long QUOTE_ID = 1L;

    @BeforeEach
    void setUp() {
        adapter = new CalculationResultJpaAdapter(
                quoteJpaRepository,
                locationJpaRepository,
                coverageOptionJpaRepository,
                calculationResultJpaRepository,
                premiumByLocationJpaRepository,
                calculationPersistenceMapper,
                locationPersistenceMapper
        );
    }

    // --- Helpers ---

    private QuoteJpa buildQuoteJpa() {
        return QuoteJpa.builder()
                .id(QUOTE_ID)
                .folioNumber(FOLIO)
                .quoteStatus("IN_PROGRESS")
                .subscriberId("SUB-001")
                .agentCode("AGT-001")
                .version(7L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private CalculationResult buildCalculationResult(boolean withCalculableLocation) {
        PremiumByLocation pbl;
        if (withCalculableLocation) {
            CoverageBreakdown bd = new CoverageBreakdown(
                    BigDecimal.valueOf(1500), BigDecimal.ZERO, BigDecimal.valueOf(105),
                    BigDecimal.valueOf(1.20), BigDecimal.valueOf(0.75), BigDecimal.valueOf(45),
                    BigDecimal.valueOf(30), BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
            pbl = new PremiumByLocation(1, "Location 1",
                    BigDecimal.valueOf(1681.95), BigDecimal.valueOf(1951.06),
                    true, bd, List.of());
        } else {
            pbl = new PremiumByLocation(1, "Incomplete",
                    null, null, false, null,
                    List.of(new BlockingAlert(BlockingAlertCode.MISSING_ZIP_CODE.name(), "Código postal requerido")));
        }
        return new CalculationResult(FOLIO, BigDecimal.valueOf(1681.95), BigDecimal.valueOf(1951.06),
                List.of(pbl), Instant.now(), 0L);
    }

    // ===== getSnapshot =====

    @Test
    void getSnapshot_returnsSnapshot_withLocationsAndCoverageOptions() {
        // GIVEN
        QuoteJpa quoteJpa = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quoteJpa));

        LocationJpa locationJpa = LocationJpa.builder()
                .id(10L).quoteId(QUOTE_ID).index(1).active(true)
                .locationName("Test").zipCode("06600")
                .businessLineCode("BL-001").businessLineFireKey("FK-001")
                .validationStatus("COMPLETE")
                .blockingAlerts(List.of())
                .build();
        when(locationJpaRepository.findByQuoteIdAndActiveTrue(QUOTE_ID)).thenReturn(List.of(locationJpa));
        when(coverageOptionJpaRepository.findAllByQuote_Id(QUOTE_ID)).thenReturn(List.of());

        Location domainLocation = new Location(
                1, true, "Test", "Av", "06600", null, null, null, null,
                null, null, null,
                new BusinessLine("BL-001", "FK-001", null),
                List.of(), null, ValidationStatus.COMPLETE, List.of()
        );
        when(locationPersistenceMapper.toDomain(locationJpa)).thenReturn(domainLocation);

        // WHEN
        QuoteCalculationSnapshot snapshot = adapter.getSnapshot(FOLIO);

        // THEN
        assertThat(snapshot.folioNumber()).isEqualTo(FOLIO);
        assertThat(snapshot.version()).isEqualTo(7L);
        assertThat(snapshot.locations()).hasSize(1);
        assertThat(snapshot.coverageOptions()).isEmpty();
    }

    @Test
    void getSnapshot_throwsFolioNotFoundException_whenFolioNotFound() {
        // GIVEN
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.empty());
        // WHEN / THEN
        assertThatThrownBy(() -> adapter.getSnapshot(FOLIO))
                .isInstanceOf(FolioNotFoundException.class);
    }

    // ===== persist =====

    @Test
    void persist_savesCalculationResult_andUpdatesQuoteStatus() {
        // GIVEN
        QuoteJpa quoteJpa = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quoteJpa));
        when(quoteJpaRepository.saveAndFlush(quoteJpa)).thenReturn(quoteJpa);
        doNothing().when(calculationResultJpaRepository).deleteByQuoteId(QUOTE_ID);

        CalculationResultJpa savedJpa = CalculationResultJpa.builder()
                .id(1L).quote(quoteJpa)
                .netPremium(BigDecimal.valueOf(1681.95))
                .commercialPremium(BigDecimal.valueOf(1951.06))
                .calculatedAt(Instant.now()).build();
        CalculationResult result = buildCalculationResult(true);

        when(calculationPersistenceMapper.toJpa(result, quoteJpa)).thenReturn(savedJpa);
        when(calculationResultJpaRepository.save(savedJpa)).thenReturn(savedJpa);
        when(calculationPersistenceMapper.toPremiumByLocationJpa(any(), eq(savedJpa)))
                .thenReturn(PremiumByLocationJpa.builder().calculable(true).locationIndex(1).build());
        when(premiumByLocationJpaRepository.saveAll(any())).thenReturn(List.of());

        // WHEN
        adapter.persist(FOLIO, result);

        // THEN
        verify(quoteJpaRepository).saveAndFlush(argThat(q -> "CALCULATED".equals(q.getQuoteStatus())));
        verify(calculationResultJpaRepository).save(savedJpa);
    }

    @Test
    void persist_deletesExistingResult_beforeSavingNew() {
        // GIVEN
        QuoteJpa quoteJpa = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quoteJpa));
        when(quoteJpaRepository.saveAndFlush(quoteJpa)).thenReturn(quoteJpa);
        doNothing().when(calculationResultJpaRepository).deleteByQuoteId(QUOTE_ID);

        CalculationResultJpa savedJpa = CalculationResultJpa.builder().id(2L).quote(quoteJpa)
                .netPremium(BigDecimal.TEN).commercialPremium(BigDecimal.TEN).calculatedAt(Instant.now()).build();
        CalculationResult result = buildCalculationResult(true);

        when(calculationPersistenceMapper.toJpa(any(), any())).thenReturn(savedJpa);
        when(calculationResultJpaRepository.save(savedJpa)).thenReturn(savedJpa);
        when(calculationPersistenceMapper.toPremiumByLocationJpa(any(), any()))
                .thenReturn(PremiumByLocationJpa.builder().calculable(true).locationIndex(1).build());
        when(premiumByLocationJpaRepository.saveAll(any())).thenReturn(List.of());

        // WHEN
        adapter.persist(FOLIO, result);

        // THEN — delete must happen before save
        var inOrder = inOrder(calculationResultJpaRepository);
        inOrder.verify(calculationResultJpaRepository).deleteByQuoteId(QUOTE_ID);
        inOrder.verify(calculationResultJpaRepository).save(savedJpa);
    }

    @Test
    void persist_savesAllPremiumByLocations() {
        // GIVEN — result with 2 locations
        QuoteJpa quoteJpa = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quoteJpa));
        when(quoteJpaRepository.saveAndFlush(quoteJpa)).thenReturn(quoteJpa);
        doNothing().when(calculationResultJpaRepository).deleteByQuoteId(QUOTE_ID);

        CoverageBreakdown bd = new CoverageBreakdown(
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        PremiumByLocation pbl1 = new PremiumByLocation(1, "L1", BigDecimal.ONE, BigDecimal.ONE, true, bd, List.of());
        PremiumByLocation pbl2 = new PremiumByLocation(2, "L2", BigDecimal.ONE, BigDecimal.ONE, true, bd, List.of());
        CalculationResult result = new CalculationResult(FOLIO, BigDecimal.valueOf(2), BigDecimal.valueOf(2.32),
                List.of(pbl1, pbl2), Instant.now(), 0L);

        CalculationResultJpa savedJpa = CalculationResultJpa.builder().id(1L).quote(quoteJpa)
                .netPremium(BigDecimal.valueOf(2)).commercialPremium(BigDecimal.valueOf(2.32))
                .calculatedAt(Instant.now()).build();
        when(calculationPersistenceMapper.toJpa(any(), any())).thenReturn(savedJpa);
        when(calculationResultJpaRepository.save(savedJpa)).thenReturn(savedJpa);
        when(calculationPersistenceMapper.toPremiumByLocationJpa(any(), eq(savedJpa)))
                .thenReturn(PremiumByLocationJpa.builder().calculable(true).locationIndex(1).build());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<PremiumByLocationJpa>> captor = ArgumentCaptor.forClass(List.class);
        when(premiumByLocationJpaRepository.saveAll(captor.capture())).thenReturn(List.of());

        // WHEN
        adapter.persist(FOLIO, result);

        // THEN
        assertThat(captor.getValue()).hasSize(2);
    }

    @Test
    void persist_savesBlockingAlerts_forNonCalculableLocations() {
        // GIVEN — result with one non-calculable location
        QuoteJpa quoteJpa = buildQuoteJpa();
        when(quoteJpaRepository.findByFolioNumber(FOLIO)).thenReturn(Optional.of(quoteJpa));
        when(quoteJpaRepository.saveAndFlush(quoteJpa)).thenReturn(quoteJpa);
        doNothing().when(calculationResultJpaRepository).deleteByQuoteId(QUOTE_ID);

        CalculationResult result = buildCalculationResult(false);
        CalculationResultJpa savedJpa = CalculationResultJpa.builder().id(1L).quote(quoteJpa)
                .netPremium(BigDecimal.ZERO).commercialPremium(BigDecimal.ZERO).calculatedAt(Instant.now()).build();

        when(calculationPersistenceMapper.toJpa(any(), any())).thenReturn(savedJpa);
        when(calculationResultJpaRepository.save(savedJpa)).thenReturn(savedJpa);
        when(calculationPersistenceMapper.toPremiumByLocationJpa(any(), any()))
                .thenReturn(PremiumByLocationJpa.builder().calculable(false).locationIndex(1).build());
        when(premiumByLocationJpaRepository.saveAll(any())).thenReturn(List.of());

        // WHEN
        adapter.persist(FOLIO, result);

        // THEN — mapper was called for the non-calculable location
        verify(calculationPersistenceMapper, times(1)).toPremiumByLocationJpa(
                argThat(pbl -> !pbl.calculable()), eq(savedJpa));
    }
}
